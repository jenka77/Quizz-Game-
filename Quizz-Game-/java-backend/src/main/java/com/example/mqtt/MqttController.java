package com.example.mqtt;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.example.auth.AuthService;
import com.example.auth.AuthService.RfidHardwareOutcome;
import com.example.auth.AuthService.RfidHardwareResult;
import com.example.controller.ControllerRepository;
import com.example.game.GameService;
import com.example.player.HeartbeatTracker;
import com.example.player.LobbyMessageService;
import com.example.player.PlayerRepository;
import com.example.player.PlayerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.MqttClient;

public class MqttController {

    private static final Logger logger = LoggerFactory.getLogger(MqttController.class);
    private static final AtomicLong lastRfidSequence = new AtomicLong(0);
    private static final long CONTROLLER_STALE_MS = 2 * 60 * 1000;
    private static final long RFID_IGNORE_AFTER_UNBIND_MS = 30_000;
    private static final Map<String, Long> recentlyUnboundControllers = new ConcurrentHashMap<>();
    private final MqttService mqttService;
    private final MqttClient mqttClient;
    private final EventBus eventBus;
    private final Vertx vertx;
    private final Set<String> activeControllerIds = ConcurrentHashMap.newKeySet();
    private Long statusTimerId;
    private Long cleanupTimerId;

    final String mqttMessagePrefix = System.getenv("MQTT_MESSAGE_PREFIX") != null ? System.getenv("MQTT_MESSAGE_PREFIX") : "test/";

    public MqttController(MqttClient mqttClient, Vertx vertx) {
        this.mqttClient = mqttClient;
        this.mqttService = new MqttService(mqttClient);
        this.eventBus = vertx.eventBus();
        this.vertx = vertx;
    }

    /* EVENTS empfangen und verarbeiten */
    public void registerEventBusConsumers() {
        this.eventBus.consumer("game.start", msg -> mqttService.publishGameStart());
        this.eventBus.consumer("game.stop", msg -> mqttService.publishGameStop());
        this.eventBus.consumer("game.question", msg -> mqttService.publishQuestion((io.vertx.core.json.JsonObject) msg.body()));

        this.eventBus.consumer("game.state.changed", msg -> {
            Object body = msg.body();
            if (body instanceof io.vertx.core.json.JsonObject) {
                io.vertx.core.json.JsonObject b = (io.vertx.core.json.JsonObject) body;
                mqttService.publishGameState(
                        b.getString("state"),
                        b.getInteger("countdownSeconds"),
                        b.getString("message"));
            }
        });
        this.eventBus.consumer("game.countdown.tick", msg -> {
            Object body = msg.body();
            if (body instanceof Number) {
                mqttService.publishCountdownTick(((Number) body).intValue());
            }
        });

        this.eventBus.consumer("heartbeat.mqtt-ping", msg -> {
            io.vertx.core.json.JsonObject body = (io.vertx.core.json.JsonObject) msg.body();
            io.vertx.core.json.JsonArray controllerIds = body.getJsonArray("controllerIds");
            if (controllerIds != null) {
                String requestId = java.util.UUID.randomUUID().toString();
                for (Object o : controllerIds) {
                    String cid = o != null ? o.toString().trim() : null;
                    if (cid != null && !cid.isBlank()) {
                        mqttService.publishPreQuestionPing(cid, requestId);
                    }
                }
            }
        });

        // Status an alle Controller pushen (Spielstart, Fragenstart) – Hardware erhält Namen + Status sofort
        this.eventBus.consumer("game.status.push.controllers", msg -> {
            Object body = msg.body();
            if (!(body instanceof io.vertx.core.json.JsonObject)) return;
            io.vertx.core.json.JsonArray arr = ((io.vertx.core.json.JsonObject) body).getJsonArray("controllerIds");
            if (arr == null) return;
            GameService gameService = new GameService();
            for (Object o : arr) {
                String cid = o != null ? o.toString().trim() : null;
                if (cid == null || cid.isBlank()) continue;
                activeControllerIds.add(cid);
                gameService.getStatus(cid).onSuccess(status -> {
                    String statusTopic = mqttMessagePrefix + "controller/" + cid + "/status";
                    mqttClient.publish(statusTopic, status.toBuffer(), io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE, false, false);
                    logger.debug("Status push (game) → {}", cid);
                }).onFailure(e -> logger.warn("Status push (game) für {}: {}", cid, e.getMessage()));
            }
        });

        // Sobald ein Hardware-Controller verbunden ist: Status (Spielername) sofort per MQTT für OLED-Anzeige senden
        this.eventBus.consumer("controller.status.push", msg -> {
            Object body = msg.body();
            String controllerId = null;
            if (body instanceof io.vertx.core.json.JsonObject) {
                controllerId = ((io.vertx.core.json.JsonObject) body).getString("controllerId");
            }
            if (controllerId == null || controllerId.isBlank()) return;
            final String cid = controllerId.trim();
            activeControllerIds.add(cid);
            new GameService().getStatus(cid).onSuccess(status -> {
                String statusTopic = mqttMessagePrefix + "controller/" + cid + "/status";
                mqttClient.publish(statusTopic, status.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
                logger.info("Status (Spielername) an Hardware gesendet: {}", cid);
            }).onFailure(e -> logger.warn("controller.status.push fehlgeschlagen für {}: {}", cid, e.getMessage()));
        });

        // Hardware aus Frontend gelöscht → aus activeControllerIds entfernen, RFID-Cooldown setzen
        this.eventBus.consumer("controller.deleted", msg -> {
            Object body = msg.body();
            String controllerId = null;
            if (body instanceof io.vertx.core.json.JsonObject) {
                controllerId = ((io.vertx.core.json.JsonObject) body).getString("controllerId");
            }
            if (controllerId != null && !controllerId.isBlank()) {
                final String cid = controllerId.trim();
                recentlyUnboundControllers.put(cid.toUpperCase(), System.currentTimeMillis());
                activeControllerIds.remove(cid);
            }
        });

        // Pre-Question-Ping an ALLE Controller (Web + Hardware) – sonst werden Web-Controller fälschlich als disconnected markiert
        this.eventBus.consumer("game.pre-question-ping", msg -> {
            io.vertx.core.json.JsonObject body = (io.vertx.core.json.JsonObject) msg.body();
            String pingId = body.getString("pingId");
            io.vertx.core.json.JsonArray controllerIds = body.getJsonArray("controllerIds");
            if (pingId != null && controllerIds != null) {
                for (Object o : controllerIds) {
                    String cid = o != null ? o.toString().trim() : null;
                    if (cid != null && !cid.isBlank()) {
                        activeControllerIds.add(cid);  // Stellt sicher, dass Hardware Status und Ping erhält
                        mqttService.publishPreQuestionPing(cid, pingId);
                    }
                }
            }
        });
    }

    public void registerMqttConsumers() {

        mqttClient.publishHandler(message -> {

            Buffer payload = message.payload();
            String topic = message.topicName();

            if (topic.endsWith("/register")) {
                String prefix = mqttMessagePrefix + "controller/";
                String suffix = "/register";
                String controllerId = topic.startsWith(prefix) && topic.endsWith(suffix) && topic.length() > prefix.length() + suffix.length()
                        ? topic.substring(prefix.length(), topic.length() - suffix.length())
                        : null;
                if (controllerId == null || controllerId.isBlank()) return;
                String cidNorm = controllerId.trim().toUpperCase();
                if (isRecentlyUnbound(cidNorm)) return;
                activeControllerIds.add(controllerId.trim());
                HeartbeatTracker.getInstance().record(controllerId.trim());
                String typeFromPayload = "WEB";
                try {
                    io.vertx.core.json.JsonObject json = new io.vertx.core.json.JsonObject(payload.toString());
                    String t = json.getString("type");
                    if (t != null && !t.isBlank()) typeFromPayload = t;
                } catch (Exception ignored) {}
                String finalType = typeFromPayload;
                logger.info("Controller-Register empfangen: {} (type={})", controllerId, finalType);
                new ControllerRepository().insertOrRefreshFreeController(controllerId, finalType)
                        .onSuccess(v -> logger.info("Controller {} in DB eingetragen/aktualisiert", controllerId))
                        .onFailure(e -> logger.warn("Controller-Register fehlgeschlagen für {}: {}", controllerId, e.getMessage()));
            } else if (topic.endsWith("/heartbeat")) {
                String prefix = mqttMessagePrefix + "controller/";
                String suffix = "/heartbeat";
                String controllerId = topic.startsWith(prefix) && topic.endsWith(suffix) && topic.length() > prefix.length() + suffix.length()
                        ? topic.substring(prefix.length(), topic.length() - suffix.length())
                        : null;
                if (controllerId == null || controllerId.isBlank()) return;
                String cidNorm = controllerId.trim().toUpperCase();
                if (isRecentlyUnbound(cidNorm)) return;
                activeControllerIds.add(controllerId.trim());
                HeartbeatTracker.getInstance().record(controllerId.trim());
                new ControllerRepository().touchController(controllerId)
                        .onFailure(e -> logger.warn("Controller-Heartbeat touch fehlgeschlagen für {}: {}", controllerId, e.getMessage()));
            } else if (topic.endsWith("/controller-state")) {
                String prefix = mqttMessagePrefix + "controller/";
                String suffix = "/controller-state";
                String controllerId = topic.startsWith(prefix) && topic.endsWith(suffix) && topic.length() > prefix.length() + suffix.length()
                        ? topic.substring(prefix.length(), topic.length() - suffix.length()).trim()
                        : null;
                if (controllerId == null || controllerId.isBlank()) return;
                activeControllerIds.add(controllerId);
                String state = null;
                try {
                    JsonObject json = new JsonObject(payload.toString());
                    state = json.getString("state");
                } catch (Exception ignored) {}
                if (state == null || state.isBlank()) return;
                logger.info("Controller-State empfangen: {} -> {} (Hardware/Web)", controllerId, state);
                final String cid = controllerId;
                final String st = state;
                new PlayerRepository().findControllerById(controllerId)
                        .compose(rows -> {
                            if (rows.size() == 0) {
                                logger.warn("Controller-State ignoriert: {} nicht in DB (registrieren/binden zuerst)", cid);
                                return io.vertx.core.Future.succeededFuture(new JsonObject());
                            }
                            if (rows.iterator().next().getLong("assigned_user_id") == null) {
                                logger.warn("Controller-State ignoriert: {} nicht gebunden – im Lobby zuweisen", cid);
                                return io.vertx.core.Future.succeededFuture(new JsonObject());
                            }
                            return new PlayerService().updateControllerReadyState(cid, st);
                        })
                        .onSuccess(v -> {
                            if (v != null && !v.isEmpty()) {
                                logger.info("Controller-State aktualisiert: {} = {}", cid, st);
                                eventBus.publish("controller.status.push", new JsonObject().put("controllerId", cid));
                            }
                        })
                        .onFailure(e -> logger.warn("Controller-State update fehlgeschlagen für {}: {}", cid, e.getMessage()));
            } else if (topic.endsWith("/answer")) {
                String prefix = mqttMessagePrefix + "controller/";
                String suffix = "/answer";
                String controllerId = topic.startsWith(prefix) && topic.endsWith(suffix) && topic.length() > prefix.length() + suffix.length()
                        ? topic.substring(prefix.length(), topic.length() - suffix.length()).trim()
                        : null;
                if (controllerId == null || controllerId.isBlank()) return;
                activeControllerIds.add(controllerId);
                String answer = null;
                try {
                    JsonObject json = new JsonObject(payload.toString());
                    answer = json.getString("answer");
                } catch (Exception ignored) {}
                if (answer == null || answer.isBlank()) return;
                logger.info("Answer empfangen via MQTT: controller={} answer={}", controllerId, answer);
                final String ansControllerId = controllerId.trim();
                final String ansLetter = answer;
                new GameService().submitAnswer(controllerId, answer)
                        .onSuccess(result -> {
                            logger.info("Answer akzeptiert: controller={} answer={}", ansControllerId, ansLetter);
                            String resultTopic = mqttMessagePrefix + "controller/" + ansControllerId + "/answer/result";
                            mqttClient.publish(resultTopic, result.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
                        })
                        .onFailure(e -> {
                            logger.warn("Answer abgelehnt: controller={} answer={} reason={}", ansControllerId, ansLetter, e.getMessage());
                            String resultTopic = mqttMessagePrefix + "controller/" + ansControllerId + "/answer/result";
                            JsonObject err = new JsonObject().put("error", e.getMessage() != null ? e.getMessage() : "answer failed");
                            mqttClient.publish(resultTopic, err.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
                        });
            } else if (topic.endsWith("/rfid")) {
                String prefix = mqttMessagePrefix + "controller/";
                String suffix = "/rfid";
                String controllerId = topic.startsWith(prefix) && topic.endsWith(suffix) && topic.length() > prefix.length() + suffix.length()
                        ? topic.substring(prefix.length(), topic.length() - suffix.length()).trim()
                        : null;
                if (controllerId == null || controllerId.isBlank()) return;
                String rfidUid = null;
                try {
                    JsonObject json = new JsonObject(payload.toString());
                    rfidUid = json.getString("rfidUid");
                } catch (Exception ignored) {}
                if (rfidUid == null || rfidUid.isBlank()) return;
                final String cid = controllerId.trim();
                long now = System.currentTimeMillis();
                Long unboundAt = recentlyUnboundControllers.get(cid.toUpperCase());
                if (unboundAt != null && (now - unboundAt) < RFID_IGNORE_AFTER_UNBIND_MS) {
                    logger.debug("RFID von kürzlich abgemeldetem Controller {} ignoriert (Cooldown)", cid);
                    return;
                }
                activeControllerIds.add(controllerId);
                HeartbeatTracker.getInstance().record(cid);
                final String rfid = rfidUid.trim();
                final long mySeq = lastRfidSequence.incrementAndGet();
                LobbyMessageService.clearRfidMessage();
                logger.info("RFID empfangen via MQTT: controller={} rfidUid={} seq={}", cid, rfid, mySeq);
                new AuthService().handleRfidFromHardware(rfid)
                        .compose(outcome -> {
                            if (lastRfidSequence.get() != mySeq) {
                                logger.debug("RFID seq {} verworfen, neueres RFID (seq {}) wurde empfangen", mySeq, lastRfidSequence.get());
                                return io.vertx.core.Future.succeededFuture();
                            }
                            if (outcome.result() == RfidHardwareResult.NOT_FOUND) {
                                LobbyMessageService.setRfidMessage("RFID nicht erkannt", true);
                                return io.vertx.core.Future.succeededFuture();
                            }
                            if (outcome.result() == RfidHardwareResult.ALREADY_IN_SESSION) {
                                LobbyMessageService.setRfidMessage("Der Spieler ist bereits in der Session", "warning");
                                return io.vertx.core.Future.succeededFuture();
                            }
                            if (outcome.result() == RfidHardwareResult.OK && outcome.authToken() != null) {
                                if (lastRfidSequence.get() != mySeq) {
                                    logger.debug("RFID seq {} OK verworfen, neueres RFID empfangen", mySeq);
                                    return io.vertx.core.Future.succeededFuture();
                                }
                                return new ControllerRepository().ensureControllerExists(cid, "HARDWARE")
                                        .compose(v -> new PlayerService().bindController(outcome.authToken(), cid, "HARDWARE"))
                                        .onSuccess(v -> {
                                            if (lastRfidSequence.get() == mySeq) {
                                                LobbyMessageService.setRfidMessage("Spieler in der Session hinzugefügt", false);
                                            }
                                            eventBus.publish("controller.status.push", new JsonObject().put("controllerId", cid));
                                            logger.info("RFID-Login erfolgreich: Spieler an Controller {} gebunden", cid);
                                        })
                                        .onFailure(e -> {
                                            if (lastRfidSequence.get() == mySeq) {
                                                logger.warn("RFID-Bind fehlgeschlagen für {}: {}", cid, e.getMessage());
                                                String msg = e.getMessage();
                                                String display = (msg != null && msg.startsWith("Bitte zuerst vom "))
                                                        ? msg
                                                        : ("Controller-Bindung fehlgeschlagen: " + (msg != null ? msg : "Unbekannt"));
                                                LobbyMessageService.setRfidMessage(display, true);
                                            }
                                        });
                            }
                            return io.vertx.core.Future.succeededFuture();
                        })
                        .onFailure(e -> {
                            if (lastRfidSequence.get() == mySeq) {
                                logger.warn("RFID-Handler fehlgeschlagen: {}", e.getMessage());
                            }
                        });
            } else if (topic.endsWith("/unbind")) {
                String prefix = mqttMessagePrefix + "controller/";
                String suffix = "/unbind";
                String controllerId = topic.startsWith(prefix) && topic.endsWith(suffix) && topic.length() > prefix.length() + suffix.length()
                        ? topic.substring(prefix.length(), topic.length() - suffix.length()).trim()
                        : null;
                if (controllerId == null || controllerId.isBlank()) return;
                final String cid = controllerId.trim();
                logger.info("Unbind (Abmelden) empfangen via MQTT: controller={}", cid);
                recentlyUnboundControllers.put(cid.toUpperCase(), System.currentTimeMillis());
                activeControllerIds.remove(cid);
                new PlayerService().unbindControllerByControllerId(cid)
                        .onSuccess(v -> logger.info("Hardware-Controller {} erfolgreich getrennt (Roter Button 2s)", cid))
                        .onFailure(e -> {
                            logger.warn("Unbind fehlgeschlagen für {}: {}", cid, e.getMessage());
                            recentlyUnboundControllers.remove(cid.toUpperCase());
                        });
            } else if (topic.endsWith("/pong")) {
                String prefix = mqttMessagePrefix + "controller/";
                String controllerId = topic.length() > prefix.length() + 5
                        ? topic.substring(prefix.length(), topic.length() - 5)
                        : null;
                if (controllerId == null || controllerId.isBlank()) return;
                try {
                    String pingId = null;
                    try {
                        io.vertx.core.json.JsonObject json = new io.vertx.core.json.JsonObject(payload.toString());
                        pingId = json.getString("requestId");
                    } catch (Exception ignored) {}
                    String finalPingId = pingId != null ? pingId : "";
                    new PlayerService().recordPreQuestionPong(controllerId, finalPingId)
                            .onFailure(v -> com.example.player.HeartbeatTracker.getInstance().record(controllerId));
                } catch (Exception e) {
                    com.example.player.HeartbeatTracker.getInstance().record(controllerId);
                }
            }

        }
        );

        mqttClient.subscribe(Map.of(
                mqttMessagePrefix + "output", 0,
                mqttMessagePrefix + "controller/+/pong", 0,
                mqttMessagePrefix + "controller/+/register", 0,
                mqttMessagePrefix + "controller/+/heartbeat", 0,
                mqttMessagePrefix + "controller/+/controller-state", 0,
                mqttMessagePrefix + "controller/+/answer", 0,
                mqttMessagePrefix + "controller/+/rfid", 0,
                mqttMessagePrefix + "controller/+/unbind", 0
        ));

        startStatusPushTimer();
        startCleanupTimer();
    }

    private synchronized void startStatusPushTimer() {
        if (statusTimerId != null) return;
        GameService gameService = new GameService();
        statusTimerId = vertx.setPeriodic(400, id -> {
            for (String controllerId : activeControllerIds) {
                if (controllerId == null || controllerId.isBlank()) continue;
                gameService.getStatus(controllerId)
                        .onSuccess(status -> {
                            String statusTopic = mqttMessagePrefix + "controller/" + controllerId.trim() + "/status";
                            mqttClient.publish(statusTopic, status.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
                        });
            }
        });
    }

    private static boolean isRecentlyUnbound(String controllerIdUpper) {
        Long unboundAt = recentlyUnboundControllers.get(controllerIdUpper);
        if (unboundAt == null) return false;
        return (System.currentTimeMillis() - unboundAt) < RFID_IGNORE_AFTER_UNBIND_MS;
    }

    private synchronized void startCleanupTimer() {
        if (cleanupTimerId != null) return;
        cleanupTimerId = vertx.setPeriodic(30_000, id -> {
            activeControllerIds.removeIf(cid ->
                    cid != null && !cid.isBlank() && HeartbeatTracker.getInstance().isStale(cid, CONTROLLER_STALE_MS));
            long cutoff = System.currentTimeMillis() - RFID_IGNORE_AFTER_UNBIND_MS;
            recentlyUnboundControllers.entrySet().removeIf(e -> e.getValue() < cutoff);
            new ControllerRepository().deleteStaleFreeControllers()
                    .onSuccess(v -> logger.debug("Stale controllers cleaned up"))
                    .onFailure(e -> logger.warn("Cleanup fehlgeschlagen: {}", e.getMessage()));
        });
    }
}
