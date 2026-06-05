package com.example.admin;

import java.util.ArrayList;
import java.util.List;

import com.example.auth.AuthService;
import com.example.http.HttpController;
import com.example.player.PlayerService;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class AdminController implements HttpController {

    private final AuthService authService;

    public AdminController() {
        this.authService = new AuthService();
    }

    @Override
    public void registerRoutes(Router router) {
        router.post("/api/admin/reset-session").handler(this::handleResetSession);
    }

    /**
     * Beendet alle Spielersessions und setzt alle Controller-Zuordnungen zurück.
     * Wird über einen Button im Frontend ausgelöst, um die Lobby für eine neue Runde zu leeren.
     */
    private void handleResetSession(RoutingContext context) {
        authService.resetAllSessionsAndControllers()
                .onSuccess(result -> {
                    JsonArray controllerIds = result.getJsonArray("controllerIds");
                    if (controllerIds != null && !controllerIds.isEmpty()) {
                        List<String> ids = new ArrayList<>();
                        for (int i = 0; i < controllerIds.size(); i++) {
                            Object o = controllerIds.getValue(i);
                            if (o != null && !o.toString().isBlank()) {
                                ids.add(o.toString().trim());
                            }
                        }
                        new PlayerService().cleanupControllerStateForControllerIds(ids);
                    }
                    context.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json")
                            .end(result.encode());
                })
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Reset fehlgeschlagen." : error.getMessage();
                    context.response().setStatusCode(500).end(message);
                });
    }
}

