package com.example.auth;

import java.util.ArrayList;
import java.util.List;

import com.example.http.HttpController;
import com.example.player.PlayerService;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class AuthController implements HttpController {

    private final AuthService authService;

    // Initialisiert den Controller mit der Auth-Service-Implementierung.
    public AuthController() {
        this.authService = new AuthService();
    }

    @Override
    // Registriert die HTTP-Route fuer die Benutzerregistrierung.
    public void registerRoutes(Router router) {
        router.post("/api/auth/register").handler(this::handleRegister);
        router.post("/api/auth/login").handler(this::handleLogin);
        router.post("/api/auth/login-rfid").handler(this::handleLoginRfid);
        router.post("/api/auth/me").handler(this::handleGetMe);
        router.post("/api/auth/logout").handler(this::handleLogout);
        router.post("/api/auth/rfid/set").handler(this::handleSetRfid);
        router.post("/api/auth/rfid/remove").handler(this::handleRemoveRfid);
    }

    // Verarbeitet die Registrierungsanfrage, liest JSON-Felder aus und liefert 201/400.
    // Ohne diesen Parameter kann die Methode nicht als Route-Handler verwendet werden.
    //status 409= conflic.wenn die anfrag ist formel ok,aber kollidiert mit bestehendem Zustand(benutzname oder RFID existiert
    //400=bad Request--> der client hat ungültige Daten gesendet

    private void handleRegister(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String username = body.getString("username");
        String password = body.getString("password");
        String rfidUid = body.getString("rfidUid");

        authService.register(username, password, rfidUid)
                .onSuccess(result -> context.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    int status = 409;
                    String message = error.getMessage() == null ? "Registrierung fehlgeschlagen." : error.getMessage();

                    if (message.contains("fehlt") || message.contains("mindestens")) {
                        status = 400;
                    }
//Sendet die Fehlermeldung als Text zurück
                    context.response().setStatusCode(status).end(message);
                });
    }

    private void handleLogin(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String username = body.getString("username");
        String password = body.getString("password");
        String rfidUid = body.getString("rfidUid");

        authService.login(username, password, rfidUid)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    int status = 401;
                    String message = error.getMessage() == null ? "Login fehlgeschlagen." : error.getMessage();

                    if (message.contains("fehlt")) {
                        status = 400;
                    } else if (message.contains("bereits vergeben")) {
                        status = 409;
                    }

                    context.response().setStatusCode(status).end(message);
                });
    }

    private void handleLoginRfid(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String rfidUid = body.getString("rfidUid");

        authService.loginRfid(rfidUid)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "RFID Login fehlgeschlagen." : error.getMessage();
                    int status = message.toLowerCase().contains("bekannt") ? 404 : 400;
                    context.response().setStatusCode(status).end(message);
                });
    }

    private void handleGetMe(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }
        String authToken = body.getString("authToken");
        authService.getCurrentUser(authToken)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "Nicht eingeloggt." : error.getMessage();
                    int status = message.contains("Nicht eingeloggt") ? 401 : 400;
                    context.response().setStatusCode(status).end(message);
                });
    }

    private void handleLogout(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String authToken = body.getString("authToken");

        authService.logout(authToken)
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
                    String message = error.getMessage() == null ? "Logout fehlgeschlagen." : error.getMessage();
                    int status = message.contains("Nicht eingeloggt") ? 401 : 400;
                    context.response().setStatusCode(status).end(message);
                });
    }

    private void handleSetRfid(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String authToken = body.getString("authToken");
        String rfidUid = body.getString("rfidUid");

        authService.setRfid(authToken, rfidUid)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "RFID setzen fehlgeschlagen." : error.getMessage();
                    int status = 400;

                    if (message.contains("Nicht eingeloggt")) {
                        status = 401;
                    } else if (message.contains("nicht gefunden")) {
                        status = 404;
                    } else if (message.contains("bereits vergeben")) {
                        status = 409;
                    }

                    context.response().setStatusCode(status).end(message);
                });
    }

    private void handleRemoveRfid(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Ungültiges JSON.");
            return;
        }

        String authToken = body.getString("authToken");

        authService.removeRfid(authToken)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> {
                    String message = error.getMessage() == null ? "RFID entfernen fehlgeschlagen." : error.getMessage();
                    int status = 400;
                    if (message.contains("Nicht eingeloggt")) {
                        status = 401;
                    } else if (message.contains("nicht gefunden")) {
                        status = 404;
                    }
                    context.response().setStatusCode(status).end(message);
                });
    }
}
