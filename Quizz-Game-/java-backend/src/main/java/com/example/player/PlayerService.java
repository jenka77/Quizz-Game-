package com.example.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.auth.AuthService;
import com.example.controller.ControllerRepository;
import com.example.game.GameStateManager;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

public class PlayerService {
    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);
    private static final int MAX_PLAYERS_PER_SESSION = 99;
    private static final long HEARTBEAT_INTERVAL_MS = 10_000;
    private static final long HEARTBEAT_STALE_MS = 20_000; // 2 missed = disconnected

    private static final java.util.Map<String, String> controllerReadyState = new java.util.concurrent.ConcurrentHashMap<>();
    private static Long heartbeatTimerId;

    private static String readyStateKey(String controllerId) {
        return (controllerId != null && !controllerId.isBlank()) ? controllerId.trim().toUpperCase(Locale.ROOT) : "";
    }

    private final PlayerRepository playerRepository;
    private final AuthService authService;

    public PlayerService() {
        this.playerRepository = new PlayerRepository();
        this.authService = new AuthService();
    }

    public Future<JsonObject> bindController(String authToken, String controllerId, String controllerType) {
        Long userId = authService.resolveUserIdForToken(authToken);
        if (userId == null) {
            return Future.failedFuture("Nicht eingeloggt.");
        }

        if (controllerId == null || controllerId.isBlank()) {
            return Future.failedFuture("Controller-ID fehlt.");
        }

        String normalizedType = normalizeControllerType(controllerType);
        if (normalizedType == null) {
            return Future.failedFuture("Controller-Typ ist ungültig.");
        }

        String normalizedControllerId = controllerId.trim();

        return playerRepository.countAssignedPlayers()
                .compose(currentPlayers -> {
                    if (currentPlayers >= MAX_PLAYERS_PER_SESSION) {
                        return Future.failedFuture(
                                "Maximal " + MAX_PLAYERS_PER_SESSION + " Spieler gleichzeitig erlaubt.");
                    }
                    return playerRepository.findControllerById(normalizedControllerId);
                })
                .compose(rows -> playerRepository.fetchControllersByUserId(userId)
                        .map(userControllers -> new Object[] { rows, userControllers }))
                .compose(pair -> {
                    @SuppressWarnings("unchecked")
                    var rows = (io.vertx.sqlclient.RowSet<Row>) ((Object[]) pair)[0];
                    @SuppressWarnings("unchecked")
                    var userControllers = (io.vertx.sqlclient.RowSet<Row>) ((Object[]) pair)[1];

                    if ("HARDWARE".equalsIgnoreCase(normalizedType) && rows.size() == 0) {
                        return Future.failedFuture("Hardware-Controller nicht gefunden.");
                    }

                    if (rows.size() > 0) {
                        Row row = rows.iterator().next();
                        Long assignedUserId = row.getLong("assigned_user_id");
                        if (assignedUserId != null && !assignedUserId.equals(userId)) {
                            return Future.failedFuture("Controller ist bereits vergeben.");
                        }
                    }

                    for (Row uc : userControllers) {
                        String existingType = uc.getString("controller_type");
                        if (existingType == null) continue;
                        String existingNorm = "WEB".equalsIgnoreCase(existingType) ? "WEB" : "HARDWARE";
                        if ("WEB".equals(existingNorm) && "HARDWARE".equals(normalizedType)) {
                            return Future.failedFuture("Bitte zuerst vom Web-Controller abmelden.");
                        }
                        if ("HARDWARE".equals(existingNorm) && "WEB".equals(normalizedType)) {
                            return Future.failedFuture("Bitte zuerst vom Hardware-Controller abmelden.");
                        }
                    }

                    return playerRepository.clearAllControllerAssignmentsForUser(userId).compose(v -> {
                        if (rows.size() == 0) {
                            return playerRepository.insertControllerAssignment(normalizedControllerId, normalizedType, userId)
                                    .map(new JsonObject()
                                            .put("message", "Controller erfolgreich gebunden.")
                                            .put("controllerId", normalizedControllerId)
                                            .put("controllerType", normalizedType)
                                            .put("userId", userId));
                        }

                        return playerRepository.updateControllerAssignment(normalizedControllerId, normalizedType, userId)
                                .map(new JsonObject()
                                        .put("message", "Controller erfolgreich gebunden.")
                                        .put("controllerId", normalizedControllerId)
                                        .put("controllerType", normalizedType)
                                        .put("userId", userId));
                    });
                }).map(result -> {
            // Nach dem Binden: Spieler ist mit Controller verbunden, aber noch weder Ready noch Not Ready markiert.
            controllerReadyState.put(readyStateKey(normalizedControllerId), "CONNECTED");
            HeartbeatTracker.getInstance().record(normalizedControllerId);
            return result;
        });
    }

    /**
     * Trennt einen Controller per ID ohne authToken (für Hardware, die vom Frontend verbunden wurde).
     * Ermöglicht Abmelden per rotem Button auch ohne RFID-Login.
     */
    public Future<JsonObject> unbindControllerByControllerId(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return Future.failedFuture("Controller-ID fehlt.");
        }
        String normalizedId = controllerId.trim();
        return playerRepository.findControllerById(normalizedId)
                .compose(rows -> {
                    Long assignedUserId = null;
                    if (rows != null && rows.size() > 0) {
                        assignedUserId = rows.iterator().next().getLong("assigned_user_id");
                    }
                    final Long userIdToInvalidate = assignedUserId;
                    return playerRepository.clearControllerAssignmentByControllerId(normalizedId)
                            .map(v -> {
                                if (userIdToInvalidate != null) {
                                    authService.invalidateSessionForUser(userIdToInvalidate);
                                }
                                return new JsonObject().put("message", "Controller-Zuordnung entfernt.");
                            });
                })
                .compose(result ->
                        new ControllerRepository().deleteControllerByControllerId(normalizedId)
                                .map(v -> result))
                .map(result -> {
                    String k = readyStateKey(normalizedId);
                    if (!k.isEmpty()) controllerReadyState.remove(k);
                    HeartbeatTracker.getInstance().remove(normalizedId);
                    GameStateManager.getInstance().onControllerStateChanged(normalizedId, "OFFLINE");
                    return result;
                });
    }

    public Future<JsonObject> unbindController(String authToken, String controllerId) {
        Long userId = authService.resolveUserIdForToken(authToken);
        if (userId == null) {
            return Future.failedFuture("Nicht eingeloggt.");
        }

        if (controllerId == null || controllerId.isBlank()) {
            return playerRepository.fetchControllersByUserId(userId)
                    .compose(rows -> {
                        for (Row row : rows) {
                            String cid = row.getString("controller_id");
                            if (cid != null && !cid.isBlank()) {
                                String n = readyStateKey(cid);
                                if (!n.isEmpty()) controllerReadyState.remove(n);
                                HeartbeatTracker.getInstance().remove(cid.trim());
                                GameStateManager.getInstance().onControllerStateChanged(n, "OFFLINE");
                            }
                        }
                        return playerRepository.clearAllControllerAssignmentsForUser(userId);
                    })
                    .map(new JsonObject().put("message", "Controller-Zuordnung entfernt."));
        }

        String normalizedControllerId = controllerId.trim();
        return playerRepository.clearControllerAssignmentForUserAndController(userId, normalizedControllerId)
                .map(new JsonObject().put("message", "Controller-Zuordnung entfernt."))
                .map(result -> {
                    String k = readyStateKey(normalizedControllerId);
                    if (!k.isEmpty()) controllerReadyState.remove(k);
                    HeartbeatTracker.getInstance().remove(normalizedControllerId);
                    GameStateManager.getInstance().onControllerStateChanged(normalizedControllerId, "OFFLINE");
                    return result;
                });
    }

    /**
     * Bereinigt den In-Memory-State für die angegebenen Controller-IDs (z. B. nach Logout).
     * Die DB-Zuordnung muss vom Aufrufer bereits gelöscht sein.
     */
    public void cleanupControllerStateForControllerIds(java.util.List<String> controllerIds) {
        if (controllerIds == null) {
            return;
        }
        for (String controllerId : controllerIds) {
            if (controllerId == null || controllerId.isBlank()) {
                continue;
            }
            String n = readyStateKey(controllerId);
            if (!n.isEmpty()) controllerReadyState.remove(n);
            HeartbeatTracker.getInstance().remove(controllerId.trim());
            GameStateManager.getInstance().onControllerStateChanged(n, "OFFLINE");
        }
    }

    public Future<JsonObject> lobbyStatus() {
        return authService.getLoggedInUserIdsSnapshot().compose(loggedInUserIds ->
                playerRepository.fetchLobbyPlayers().compose(rows -> {
            io.vertx.core.json.JsonArray players = new io.vertx.core.json.JsonArray();
            Set<Long> alreadyIncludedUserIds = new HashSet<>();

            for (Row row : rows) {
                String controllerStatus = row.getString("status");
                boolean connected = controllerStatus != null && controllerStatus.equalsIgnoreCase("ASSIGNED");
                String controllerId = row.getString("controller_id");
                Long userId = row.getLong("user_id");
                String readyState = getControllerReadyState(controllerId);
                boolean ready = "READY".equalsIgnoreCase(readyState);
                boolean notReady = "NOT_READY".equalsIgnoreCase(readyState);

                players.add(new JsonObject()
                        .put("userId", userId)
                        .put("name", row.getString("username"))
                        .put("controllerId", controllerId)
                        .put("controllerType", row.getString("controller_type"))
                        .put("connected", connected)
                        .put("ready", ready)
                        .put("notReady", notReady));
                alreadyIncludedUserIds.add(userId);
            }

            java.util.List<Long> missingLoggedInUserIds = new ArrayList<>();
            for (Long userId : loggedInUserIds) {
                if (!alreadyIncludedUserIds.contains(userId)) {
                    missingLoggedInUserIds.add(userId);
                }
            }

            if (missingLoggedInUserIds.isEmpty()) {
                return Future.succeededFuture(buildLobbyStatusResponse(players));
            }

            // Spieler ohne zugewiesenen Controller = noch nicht "connected" (ProjektBeschreibung)
            return playerRepository.fetchUsersByIds(missingLoggedInUserIds).map(userRows -> {
                for (Row userRow : userRows) {
                    players.add(new JsonObject()
                            .put("userId", userRow.getLong("id"))
                            .put("name", userRow.getString("username"))
                            .put("controllerId", null)
                            .put("controllerType", "NONE")
                            .put("connected", false)
                            .put("ready", false)
                            .put("notReady", false));
                }

                return buildLobbyStatusResponse(players);
            });
        }));
    }

    private static JsonObject buildLobbyStatusResponse(io.vertx.core.json.JsonArray players) {
        JsonObject response = new JsonObject().put("players", players);
        LobbyMessageService.LobbyMessage msg = LobbyMessageService.getLastMessage();
        if (msg != null) {
            response.put("lobbyMessage", msg.text())
                    .put("lobbyMessageType", msg.type())
                    .put("lobbyMessageId", msg.id());
        }
        return response;
    }

    public Future<JsonObject> updateControllerReadyState(String controllerId, String state) {
        if (controllerId == null || controllerId.isBlank()) {
            return Future.failedFuture("Controller-ID fehlt.");
        }
        if (state == null || state.isBlank()) {
            return Future.failedFuture("State fehlt.");
        }

        String normalizedControllerId = controllerId.trim();
        String stateKey = readyStateKey(controllerId);
        String normalizedState = state.trim().toUpperCase();

        // Joueurs: Connected, Disconnected, Ready, Not Ready
        // Web-Controller (Liste): Available oder Unavailable
        String internalState;
        String dbStatus;
        switch (normalizedState) {
            case "READY", "AVAILABLE" -> {
                internalState = "READY";
                dbStatus = "ASSIGNED";
            }
            case "NOT_READY", "UNAVAILABLE" -> {
                internalState = "NOT_READY";
                dbStatus = "ASSIGNED";
            }
            case "CONNECTED" -> {
                internalState = "CONNECTED";
                dbStatus = "ASSIGNED";
            }
            case "OFFLINE" -> {
                internalState = "NOT_READY";
                dbStatus = "OFFLINE";
            }
            default -> {
                return Future.failedFuture("Ungültiger State.");
            }
        }

        return playerRepository.updateControllerStatusByControllerId(normalizedControllerId, dbStatus)
                .map(new JsonObject()
                        .put("message", "Controller-Status aktualisiert.")
                        .put("controllerId", normalizedControllerId)
                        .put("state", internalState))
                .map(result -> {
                    if ("OFFLINE".equals(normalizedState)) {
                        controllerReadyState.put(stateKey, "NOT_READY");
                        HeartbeatTracker.getInstance().remove(normalizedControllerId);
                    } else {
                        controllerReadyState.put(stateKey, internalState);
                        HeartbeatTracker.getInstance().record(normalizedControllerId);
                    }

                    // OFFLINE an GameStateManager übergeben, damit der Spieler aus activeResponderUserIds entfernt wird
                    String stateForGame = "OFFLINE".equals(normalizedState) ? "OFFLINE" : internalState;
                    GameStateManager.getInstance().onControllerStateChanged(normalizedControllerId, stateForGame);

                    if (("NOT_READY".equals(internalState) || "OFFLINE".equals(normalizedState))
                            && GameStateManager.getInstance().isCountdownRunning()) {
                        GameStateManager.getInstance().cancelCountdown("Countdown abgebrochen. Ein Spieler ist wieder nicht ready.");
                    }
                    return result;
                });
    }

    public Future<JsonObject> kickPlayer(String authToken, Long targetUserId) {
        Long requesterUserId = authService.resolveUserIdForToken(authToken);
        if (requesterUserId == null) {
            return Future.failedFuture("Nicht eingeloggt.");
        }
        if (targetUserId == null || targetUserId <= 0) {
            return Future.failedFuture("Zielspieler ist ungültig.");
        }
        if (requesterUserId.equals(targetUserId)) {
            return Future.failedFuture("Nutze Abmelden für den eigenen Account.");
        }

        return playerRepository.fetchControllersByUserId(targetUserId).compose(rows -> {
            for (Row row : rows) {
                String controllerId = row.getString("controller_id");
                if (controllerId == null || controllerId.isBlank()) {
                    continue;
                }

                String k = readyStateKey(controllerId);
                if (!k.isEmpty()) controllerReadyState.remove(k);
                GameStateManager.getInstance().onControllerStateChanged(controllerId, "OFFLINE");
            }

            return playerRepository.clearAllControllerAssignmentsForUser(targetUserId);
        }).map(v -> {
            boolean hadSession = authService.invalidateSessionForUser(targetUserId);
            return new JsonObject()
                    .put("message", hadSession ? "Spieler wurde aus der Session entfernt." : "Spieler war bereits ausgeloggt.")
                    .put("targetUserId", targetUserId);
        });
    }

    public static String getControllerReadyState(String controllerId) {
        String key = readyStateKey(controllerId);
        return key.isEmpty() ? "CONNECTED" : controllerReadyState.getOrDefault(key, "CONNECTED");
    }

    public Future<JsonObject> processHeartbeat(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return Future.failedFuture("Controller-ID fehlt.");
        }
        String normalized = controllerId.trim();
        return playerRepository.findControllerById(normalized)
                .compose(rows -> {
                    if (rows.size() == 0) {
                        return Future.failedFuture("Controller nicht gefunden.");
                    }
                    Row row = rows.iterator().next();
                    if (row.getLong("assigned_user_id") == null) {
                        return Future.failedFuture("Controller ist nicht zugewiesen.");
                    }
                    HeartbeatTracker.getInstance().record(normalized);
                    return Future.succeededFuture(new JsonObject()
                            .put("message", "Heartbeat erhalten.")
                            .put("controllerId", normalized));
                });
    }

    private static Vertx heartbeatVertx;

    public static synchronized void startHeartbeatTimer(Vertx vertx) {
        if (heartbeatTimerId != null) {
            return;
        }
        heartbeatVertx = vertx;
        PlayerService service = new PlayerService();
        heartbeatTimerId = vertx.setPeriodic(HEARTBEAT_INTERVAL_MS, id -> {
            service.checkAndMarkStaleHeartbeats();
        });
    }

    /**
     * Regulär: Alle 10 Sekunden (2 verpasste = disconnected).
     * Sendet Ping an ALLE zugewiesenen Controller (Web + Hardware), nicht nur Hardware.
     */
    public void checkAndMarkStaleHeartbeats() {
        playerRepository.fetchAssignedControllerIds()
                .onSuccess(controllerIds -> {
                    List<String> controllersToPing = new ArrayList<>();
                    for (String controllerId : controllerIds) {
                        if (HeartbeatTracker.getInstance().isStale(controllerId, HEARTBEAT_STALE_MS)) {
                            logger.warn("Controller {} hat 2 Heartbeats verpasst – markiere als disconnected.", controllerId);
                            markControllerOffline(controllerId);
                        } else {
                            controllersToPing.add(controllerId);
                        }
                    }
                    if (!controllersToPing.isEmpty() && heartbeatVertx != null) {
                        heartbeatVertx.eventBus()
                                .publish("heartbeat.mqtt-ping", new JsonObject().put("controllerIds", new io.vertx.core.json.JsonArray(controllersToPing)));
                    }
                })
                .onFailure(err -> logger.warn("Heartbeat-Check fehlgeschlagen: {}", err.getMessage()));
    }

    public Future<JsonObject> recordPreQuestionPong(String controllerId, String pingId) {
        if (controllerId == null || controllerId.isBlank()) {
            return Future.failedFuture("Controller-ID fehlt.");
        }
        String normalized = controllerId.trim();
        boolean ok = GameStateManager.getInstance().recordPreQuestionPong(normalized, pingId);
        if (!ok) {
            return Future.failedFuture("Kein aktiver Pre-Question-Ping oder falsche Ping-ID.");
        }
        HeartbeatTracker.getInstance().record(normalized);
        return Future.succeededFuture(new JsonObject()
                .put("message", "Pre-Question-Pong erhalten.")
                .put("controllerId", normalized));
    }

    private void markControllerOffline(String controllerId) {
        playerRepository.updateControllerStatusByControllerId(controllerId, "OFFLINE")
                .onSuccess(v -> {
                    String k = readyStateKey(controllerId);
                    if (!k.isEmpty()) controllerReadyState.put(k, "NOT_READY");
                    HeartbeatTracker.getInstance().remove(controllerId);
                    GameStateManager.getInstance().onControllerStateChanged(controllerId, "OFFLINE");
                })
                .onFailure(err -> logger.warn("Controller {} konnte nicht offline gesetzt werden: {}", controllerId, err.getMessage()));
    }

    public void markControllerOfflineFromPreQuestion(String controllerId) {
        playerRepository.updateControllerStatusByControllerId(controllerId, "OFFLINE")
                .onSuccess(v -> {
                    String k = readyStateKey(controllerId);
                    if (!k.isEmpty()) controllerReadyState.put(k, "NOT_READY");
                    HeartbeatTracker.getInstance().remove(controllerId);
                })
                .onFailure(err -> logger.warn("Controller {} (Pre-Question-Timeout) konnte nicht offline gesetzt werden: {}", controllerId, err.getMessage()));
    }

    private String normalizeControllerType(String controllerType) {
        if (controllerType == null || controllerType.isBlank()) {
            return null;
        }

        String normalized = controllerType.trim().toUpperCase();
        if (normalized.equals("WEB-CONTROLLER") || normalized.equals("WEB")) {
            return "WEB";
        }
        if (normalized.equals("HARDWARE-CONTROLLER") || normalized.equals("HARDWARE")) {
            return "HARDWARE";
        }

        return null;
    }
}
