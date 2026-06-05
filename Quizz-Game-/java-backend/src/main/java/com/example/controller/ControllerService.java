package com.example.controller;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

public class ControllerService {

    private final ControllerRepository controllerRepository;

    public ControllerService() {
        this.controllerRepository = new ControllerRepository();
    }

    /**
     * Registriert einen Controller in der DB (Status FREE), damit er in der Liste erscheint.
     * Wird von der controller-config-Seite genutzt, wenn beim Öffnen eine ID erzeugt wird.
     */
    public Future<Void> registerFreeController(String controllerId, String controllerType) {
        return controllerRepository.insertOrRefreshFreeController(
                controllerId != null ? controllerId.trim() : null,
                controllerType != null ? controllerType : "WEB"
        );
    }

    public Future<JsonObject> availableControllers() {
        return controllerRepository.fetchRecentOrAssignedControllers()
                .compose(webRows -> controllerRepository.fetchRecentHardwareControllers()
                        .map(hwRows -> {
                            JsonArray controllers = new JsonArray();
                            JsonArray hardwareControllers = new JsonArray();

                            for (Row row : webRows) {
                                String status = row.getString("status");
                                boolean available = "FREE".equalsIgnoreCase(status);
                                String displayStatus = available ? "AVAILABLE" : "UNAVAILABLE";

                                JsonObject item = new JsonObject()
                                        .put("controllerId", row.getString("controller_id"))
                                        .put("controllerType", row.getString("controller_type"))
                                        .put("status", displayStatus)
                                        .put("available", available)
                                        .put("assignedUserId", row.getLong("assigned_user_id"))
                                        .put("assignedUsername", row.getString("assigned_username"));

                                controllers.add(item);
                            }

                            // Hardware: nur kürzlich empfangene IDs (last_seen < 1 min)
                            for (Row row : hwRows) {
                                String status = row.getString("status");
                                boolean available = "FREE".equalsIgnoreCase(status);
                                String displayStatus = available ? "AVAILABLE" : "UNAVAILABLE";

                                hardwareControllers.add(new JsonObject()
                                        .put("controllerId", row.getString("controller_id"))
                                        .put("controllerType", row.getString("controller_type"))
                                        .put("status", displayStatus)
                                        .put("available", available)
                                        .put("assignedUserId", row.getLong("assigned_user_id"))
                                        .put("assignedUsername", row.getString("assigned_username")));
                            }

                            return new JsonObject()
                                    .put("webUnlimited", true)
                                    .put("controllers", controllers)
                                    .put("hardwareControllers", hardwareControllers);
                        }));
    }
}
