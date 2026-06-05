package com.example.player;

import com.example.http.HttpController;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class PlayerController implements HttpController
{
    private final PlayerService playerService;

    public PlayerController() {
        this.playerService = new PlayerService();
    }

    @Override
    public void registerRoutes(Router router) {
        router.post("/api/players/bind").handler(this::handleBind);
        router.post("/api/players/unbind").handler(this::handleUnbind);
        router.post("/api/controllers/unbind-hardware").handler(this::handleUnbindHardware);
        router.post("/api/controllers/delete-hardware").handler(this::handleDeleteHardware);
        router.post("/api/players/kick").handler(this::handleKick);
        router.post("/api/players/controller-state").handler(this::handleControllerState);
        router.post("/api/players/heartbeat").handler(this::handleHeartbeat);
        router.post("/api/players/pre-question-pong").handler(this::handlePreQuestionPong);
        router.get("/api/lobby/status").handler(this::handleLobbyStatus);
    }

    private void handleBind(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String authToken = body.getString("authToken");
        String controllerId = body.getString("controllerId");
        String controllerType = body.getString("controllerType");

        playerService.bindController(authToken, controllerId, controllerType)
                .onSuccess(result -> {
                    // Spielername sofort per MQTT an Hardware senden für OLED-Anzeige
                    if ("HARDWARE".equalsIgnoreCase(result.getString("controllerType"))) {
                        String cid = result.getString("controllerId");
                        if (cid != null && !cid.isBlank()) {
                            context.vertx().eventBus().publish("controller.status.push",
                                    new JsonObject().put("controllerId", cid));
                        }
                    }
                    context.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json")
                            .end(result.encode());
                })
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Controller-Bindung fehlgeschlagen." : error.getMessage();
                    int status = 400;
                    if (message.contains("Nicht eingeloggt")) {
                        status = 401;
                    } else if (message.contains("vergeben")) {
                        status = 409;
                    } else if (message.contains("Maximal")) {
                        status = 409;
                    }
                    context.response().setStatusCode(status).end(message);
                });
    }

    /**
     * Unbind nur per controllerId (für Hardware vom Frontend verbunden, ohne authToken).
     * Ermöglicht Abmelden per rotem Button auch ohne RFID-Login.
     */
    private void handleUnbindHardware(RoutingContext context) {
        JsonObject body;
        try {
            body = context.body().asJsonObject();
        } catch (Exception e) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }
        String controllerId = body.getString("controllerId");
        if (controllerId == null || controllerId.isBlank()) {
            context.response().setStatusCode(400).end("controllerId fehlt.");
            return;
        }
        final String cid = controllerId.trim();
        playerService.unbindControllerByControllerId(cid)
                .onSuccess(result -> {
                    context.vertx().eventBus().publish("controller.deleted", new JsonObject().put("controllerId", cid));
                    context.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json")
                            .end(result.encode());
                })
                .onFailure(error -> {
                    String msg = error.getMessage() != null ? error.getMessage() : "Unbind fehlgeschlagen.";
                    context.response().setStatusCode(400).end(msg);
                });
    }

    private void handleDeleteHardware(RoutingContext context) {
        JsonObject body;
        try {
            if (context.body() == null) {
                context.response().setStatusCode(400).end("Kein Request-Body.");
                return;
            }
            body = context.body().asJsonObject();
        } catch (Exception e) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }
        String controllerId = body.getString("controllerId");
        if (controllerId == null || controllerId.isBlank()) {
            context.response().setStatusCode(400).end("controllerId fehlt.");
            return;
        }
        final String cid = controllerId.trim();
        playerService.unbindControllerByControllerId(cid)
                .onSuccess(result -> {
                    context.vertx().eventBus().publish("controller.deleted", new JsonObject().put("controllerId", cid));
                    context.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject().put("message", "Hardware-Controller gelöscht.").encode());
                })
                .onFailure(error -> {
                    String msg = error.getMessage() != null ? error.getMessage() : "Löschen fehlgeschlagen.";
                    context.response().setStatusCode(400).end(msg);
                });
    }

    private void handleUnbind(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String authToken = body.getString("authToken");
        String controllerId = body.getString("controllerId");

        playerService.unbindController(authToken, controllerId)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Controller-Unbind fehlgeschlagen." : error.getMessage();
                    int status = message.contains("Nicht eingeloggt") ? 401 : 400;
                    context.response().setStatusCode(status).end(message);
                });
    }

    private void handleControllerState(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String controllerId = body.getString("controllerId");
        String state = body.getString("state");

        playerService.updateControllerReadyState(controllerId, state)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Controller-State fehlgeschlagen." : error.getMessage();
                    context.response().setStatusCode(400).end(message);
                });
    }

    private void handleKick(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String authToken = body.getString("authToken");
        Long targetUserId = body.getLong("targetUserId");

        if (targetUserId == null) {
            Number numericTarget = body.getNumber("targetUserId");
            if (numericTarget != null) {
                targetUserId = numericTarget.longValue();
            }
        }

        playerService.kickPlayer(authToken, targetUserId)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Kick fehlgeschlagen." : error.getMessage();
                    int status = 400;
                    if (message.contains("Nicht eingeloggt")) {
                        status = 401;
                    } else if (message.contains("Abmelden")) {
                        status = 403;
                    }
                    context.response().setStatusCode(status).end(message);
                });
    }

    private void handleHeartbeat(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }
        String controllerId = body.getString("controllerId");
        playerService.processHeartbeat(controllerId)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Heartbeat fehlgeschlagen." : error.getMessage();
                    context.response().setStatusCode(400).end(message);
                });
    }

    private void handlePreQuestionPong(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }
        String controllerId = body.getString("controllerId");
        String pingId = body.getString("pingId");
        playerService.recordPreQuestionPong(controllerId, pingId)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Pre-Question-Pong fehlgeschlagen." : error.getMessage();
                    context.response().setStatusCode(400).end(message);
                });
    }

    private void handleLobbyStatus(RoutingContext context) {
        playerService.lobbyStatus()
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Lobby-Status fehlgeschlagen." : error.getMessage();
                    context.response().setStatusCode(500).end(message);
                });
    }

}
