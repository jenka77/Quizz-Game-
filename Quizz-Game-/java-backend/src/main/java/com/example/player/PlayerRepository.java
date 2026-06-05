package com.example.player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class PlayerRepository {

    private final JDBCPool jdbcPool;

    public PlayerRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    public Future<RowSet<Row>> findControllerById(String controllerId) {
        return jdbcPool.preparedQuery("""
                SELECT id, controller_id, controller_type, status, assigned_user_id
                FROM controllers
                WHERE controller_id = ?
                """).execute(Tuple.of(controllerId));
    }

    /** Benutzername des dem Controller zugewiesenen Spielers (für Hardware-/OLED-Anzeige). */
    public Future<String> findAssignedUsernameByControllerId(String controllerId) {
        return jdbcPool.preparedQuery("""
                SELECT u.username FROM controllers c
                JOIN users u ON u.id = c.assigned_user_id
                WHERE c.controller_id = ?
                """).execute(Tuple.of(controllerId))
                .map(rows -> {
                    var it = rows.iterator();
                    return it.hasNext() ? it.next().getString("username") : null;
                })
                .recover(e -> io.vertx.core.Future.succeededFuture((String) null));
    }

    public Future<Void> insertControllerAssignment(String controllerId, String controllerType, Long userId) {
        return jdbcPool.preparedQuery("""
                INSERT INTO controllers (controller_id, controller_type, status, assigned_user_id, last_seen_at)
                VALUES (?, ?, 'ASSIGNED', ?, CURRENT_TIMESTAMP)
                """).execute(Tuple.of(controllerId, controllerType, userId)).mapEmpty();
    }

    public Future<Void> updateControllerAssignment(String controllerId, String controllerType, Long userId) {
        return jdbcPool.preparedQuery("""
                UPDATE controllers
                SET controller_type = ?, status = 'ASSIGNED', assigned_user_id = ?, last_seen_at = CURRENT_TIMESTAMP
                WHERE controller_id = ?
                """).execute(Tuple.of(controllerType, userId, controllerId)).mapEmpty();
    }

    public Future<Void> clearControllerAssignmentForUserAndController(Long userId, String controllerId) {
        return jdbcPool.preparedQuery("""
                UPDATE controllers
                SET assigned_user_id = NULL, status = 'FREE'
                WHERE assigned_user_id = ? AND controller_id = ?
                """).execute(Tuple.of(userId, controllerId)).mapEmpty();
    }

    /** Trennt einen Controller per ID (für Hardware ohne authToken, z.B. vom Frontend verbunden). */
    public Future<Void> clearControllerAssignmentByControllerId(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return io.vertx.core.Future.failedFuture("Controller-ID fehlt.");
        }
        return jdbcPool.preparedQuery("""
                UPDATE controllers
                SET assigned_user_id = NULL, status = 'FREE', last_seen_at = CURRENT_TIMESTAMP
                WHERE controller_id = ?
                """).execute(Tuple.of(controllerId.trim())).mapEmpty();
    }

    /** Entfernt nur die Zuordnungen dieses einen Benutzers; andere Spieler in der Lobby bleiben zugeordnet. */
    public Future<Void> clearAllControllerAssignmentsForUser(Long userId) {
        return jdbcPool.preparedQuery("""
                UPDATE controllers
                SET assigned_user_id = NULL, status = 'FREE'
                WHERE assigned_user_id = ?
                """).execute(Tuple.of(userId)).mapEmpty();
    }

    public Future<RowSet<Row>> fetchLobbyPlayers() {
        return jdbcPool.preparedQuery("""
                SELECT
                    u.id AS user_id,
                    u.username,
                    c.controller_id,
                    c.controller_type,
                    c.status
                FROM controllers c
                JOIN users u ON u.id = c.assigned_user_id
                ORDER BY u.username ASC
                """).execute();
    }

    public Future<Integer> countAssignedPlayers() {
        return jdbcPool.preparedQuery("""
                SELECT COUNT(DISTINCT assigned_user_id) AS total
                FROM controllers
                WHERE assigned_user_id IS NOT NULL
                  AND status = 'ASSIGNED'
                """).execute().map(rows -> {
            Row row = rows.iterator().next();
            Number total = (Number) row.getValue("total");
            return total == null ? 0 : total.intValue();
        });
    }

    public Future<RowSet<Row>> fetchUsersByIds(List<Long> userIds) {
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
                SELECT id, username
                FROM users
                WHERE id IN (%s)
                ORDER BY username ASC
                """.formatted(placeholders);

        Tuple tuple = Tuple.tuple();
        for (Long userId : userIds) {
            tuple.addValue(userId);
        }

        return jdbcPool.preparedQuery(sql).execute(tuple);
    }

    /**
     * Löscht alle Controller-Zuordnungen (setzt alle Controller auf FREE, ohne sie zu entfernen).
     */
    public Future<Void> clearAllControllerAssignments() {
        return jdbcPool.preparedQuery("""
                UPDATE controllers
                SET assigned_user_id = NULL, status = 'FREE'
                """).execute().mapEmpty();
    }

    /**
     * Entfernt alle Controller-Einträge vollständig (für einen harten Reset).
     */
    public Future<Void> deleteAllControllers() {
        return jdbcPool.preparedQuery("""
                DELETE FROM controllers
                """).execute().mapEmpty();
    }

    public Future<Void> updateControllerStatusByControllerId(String controllerId, String status) {
        return jdbcPool.preparedQuery("""
                UPDATE controllers
                SET status = ?, last_seen_at = CURRENT_TIMESTAMP
                WHERE controller_id = ?
                """).execute(Tuple.of(status, controllerId)).mapEmpty();
    }

    public Future<RowSet<Row>> fetchControllersByUserId(Long userId) {
        return jdbcPool.preparedQuery("""
                SELECT controller_id, controller_type
                FROM controllers
                WHERE assigned_user_id = ?
                """).execute(Tuple.of(userId));
    }

    public Future<List<String>> fetchAssignedControllerIds() {
        return jdbcPool.preparedQuery("""
                SELECT controller_id
                FROM controllers
                WHERE assigned_user_id IS NOT NULL
                  AND status = 'ASSIGNED'
                """).execute()
                .map(rows -> {
                    List<String> ids = new ArrayList<>();
                    for (Row row : rows) {
                        String id = row.getString("controller_id");
                        if (id != null && !id.isBlank()) {
                            ids.add(id.trim().toUpperCase());
                        }
                    }
                    return ids;
                });
    }

    /** Alle controller_id aus der Tabelle (für Reset: Fenster schließen + In-Memory bereinigen). */
    public Future<List<String>> fetchAllControllerIds() {
        return jdbcPool.preparedQuery("""
                SELECT controller_id FROM controllers
                """).execute()
                .map(rows -> {
                    List<String> ids = new ArrayList<>();
                    for (Row row : rows) {
                        String id = row.getString("controller_id");
                        if (id != null && !id.isBlank()) {
                            ids.add(id.trim());
                        }
                    }
                    return ids;
                });
    }

}
