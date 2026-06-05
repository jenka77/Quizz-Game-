package com.example.controller;

import com.example.http.HttpController;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ControllerController implements HttpController {

    private final ControllerService controllerService;

    public ControllerController() {
        this.controllerService = new ControllerService();
    }

    @Override
    public void registerRoutes(Router router) {
        router.get("/api/controllers/available").handler(this::handleAvailableControllers);
        router.post("/api/controllers/register").handler(this::handleRegisterController);
    }

    private void handleRegisterController(RoutingContext context) {
        context.request().bodyHandler(body -> {
            JsonObject json = body.toJsonObject();
            String controllerId = json.getString("controllerId");
            String controllerType = json.getString("controllerType");
            if (controllerId == null || controllerId.isBlank()) {
                context.response().setStatusCode(400).end("controllerId fehlt.");
                return;
            }
            controllerService.registerFreeController(controllerId, controllerType != null ? controllerType : "WEB")
                    .onSuccess(v -> context.response().setStatusCode(200).putHeader("content-type", "application/json").end("{}"))
                    .onFailure(err -> context.response().setStatusCode(500).end(err.getMessage() != null ? err.getMessage() : "Fehler"));
        });
    }

    private void handleAvailableControllers(RoutingContext context) {
        controllerService.availableControllers()
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Controller-Liste fehlgeschlagen." : error.getMessage();
                    context.response().setStatusCode(500).end(message);
                });
    }
}
