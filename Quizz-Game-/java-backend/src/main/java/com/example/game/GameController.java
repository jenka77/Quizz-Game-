package com.example.game;

import com.example.http.HttpController;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class GameController implements HttpController {

    private final GameService gameService;

    public GameController() {
        this.gameService = new GameService();
    }

    @Override
    public void registerRoutes(Router router) {
        router.get("/api/game/config").handler(this::handleLoadConfig);
        router.post("/api/game/config").handler(this::handleSaveConfig);
        router.get("/api/game/status").handler(this::handleStatus);
        router.post("/api/game/start").handler(this::handleStart);
        router.post("/api/game/answer").handler(this::handleAnswer);
    }

    private void handleLoadConfig(RoutingContext context) {
        gameService.loadConfig()
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> context.response().setStatusCode(500).end(error.getMessage()));
    }

    private void handleSaveConfig(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        gameService.saveConfig(body)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> context.response().setStatusCode(400).end(error.getMessage()));
    }

    private void handleStatus(RoutingContext context) {
        String controllerId = context.request().getParam("controllerId");
        gameService.getStatus(controllerId)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> context.response().setStatusCode(500).end(error.getMessage()));
    }

    private void handleStart(RoutingContext context) {
        gameService.startGame()
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> context.response().setStatusCode(400).end(error.getMessage()));
    }

    private void handleAnswer(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String controllerId = body.getString("controllerId");
        String answer = body.getString("answer");

        gameService.submitAnswer(controllerId, answer)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> context.response().setStatusCode(400).end(error.getMessage()));
    }
}
