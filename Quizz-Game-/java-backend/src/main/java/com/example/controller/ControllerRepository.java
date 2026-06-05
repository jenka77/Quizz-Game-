package com.example.controller;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class ControllerRepository {

    private final JDBCPool jdbcPool;

    public ControllerRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    public Future<Void> insertOrRefreshFreeController(String controllerId, String controllerType) {
        if (controllerId == null || controllerId.isBlank()) {
            return Future.failedFuture("Controller-ID fehlt.");
        }
        String normalizedId = controllerId.trim();
        String normalizedType = (controllerType != null && controllerType.toUpperCase().startsWith("WEB")) ? "WEB" : "HARDWARE";

        // INSERT neuer Controller (FREE) oder UPDATE falls bereits vorhanden.
        // HARDWARE: wenn seit 2 min nicht gesehen -> FREE (Wiederverbindung). Sonst Status beibehalten.
        // WICHTIG: Beim RFID-Empfang sendet der Controller aktiv – last_seen_at wird auf JETZT gesetzt,
        // daher wird assigned_user_id NICHT gelöscht (Spieler bleibt in Spielstatus sichtbar).
        return jdbcPool.preparedQuery("""
                INSERT INTO controllers (controller_id, controller_type, status, last_seen_at)
                VALUES (?, ?, 'FREE', CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                  last_seen_at = CURRENT_TIMESTAMP,
                  status = IF(controller_type = 'HARDWARE'
                    AND (assigned_user_id IS NULL
                         OR last_seen_at IS NULL
                         OR last_seen_at < CURRENT_TIMESTAMP - INTERVAL 2 MINUTE),
                    'FREE', status),
                  assigned_user_id = IF(controller_type = 'HARDWARE'
                    AND (last_seen_at IS NULL OR last_seen_at < CURRENT_TIMESTAMP - INTERVAL 2 MINUTE),
                    NULL, assigned_user_id)
                """).execute(Tuple.of(normalizedId, normalizedType)).mapEmpty();
    }

    /**
     * Stellt sicher, dass der Controller existiert, ohne assigned_user_id zu löschen.
     * Wird vor RFID-Bind verwendet, damit der Spieler sofort im Spielstatus erscheint.
     */
    public Future<Void> ensureControllerExists(String controllerId, String controllerType) {
        if (controllerId == null || controllerId.isBlank()) {
            return Future.failedFuture("Controller-ID fehlt.");
        }
        String normalizedId = controllerId.trim();
        String normalizedType = (controllerType != null && controllerType.toUpperCase().startsWith("WEB")) ? "WEB" : "HARDWARE";
        return jdbcPool.preparedQuery("""
                INSERT INTO controllers (controller_id, controller_type, status, last_seen_at)
                VALUES (?, ?, 'FREE', CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE last_seen_at = CURRENT_TIMESTAMP
                """).execute(Tuple.of(normalizedId, normalizedType)).mapEmpty();
    }

    public Future<Void> touchController(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return Future.failedFuture("Controller-ID fehlt.");
        }
        String normalizedId = controllerId.trim();
        return jdbcPool.preparedQuery("""
                UPDATE controllers
                SET last_seen_at = CURRENT_TIMESTAMP
                WHERE controller_id = ?
                """).execute(Tuple.of(normalizedId)).mapEmpty();
    }

    public Future<RowSet<Row>> fetchAllControllers() {
        return jdbcPool.preparedQuery("""
                SELECT
                    c.controller_id,
                    c.controller_type,
                    c.status,
                    c.assigned_user_id,
                    u.username AS assigned_username
                FROM controllers c
                LEFT JOIN users u ON u.id = c.assigned_user_id
                ORDER BY c.controller_type ASC, c.controller_id ASC
                """).execute();
    }

    /**
     * Gibt nur kürzlich gesehene Controller (last_seen &lt; 1 min) oder bereits zugewiesene zurück.
     * So erscheinen nur aktive Controller (RFID/Heartbeat gesendet) oder verbundene.
     * Ältere freie Einträge verschwinden aus der Liste.
     */
    public Future<RowSet<Row>> fetchRecentOrAssignedControllers() {
        return jdbcPool.preparedQuery("""
                SELECT
                    c.controller_id,
                    c.controller_type,
                    c.status,
                    c.assigned_user_id,
                    u.username AS assigned_username
                FROM controllers c
                LEFT JOIN users u ON u.id = c.assigned_user_id
                WHERE c.last_seen_at > (CURRENT_TIMESTAMP - INTERVAL 1 MINUTE)
                   OR c.assigned_user_id IS NOT NULL
                ORDER BY c.controller_type ASC, c.controller_id ASC
                """).execute();
    }

    /**
     * Gibt nur kürzlich gesehene HARDWARE-Controller zurück (last_seen &lt; 1 min).
     * Ältere Hardware (frei oder zugewiesen) wird nicht angezeigt.
     */
    public Future<RowSet<Row>> fetchRecentHardwareControllers() {
        return jdbcPool.preparedQuery("""
                SELECT
                    c.controller_id,
                    c.controller_type,
                    c.status,
                    c.assigned_user_id,
                    u.username AS assigned_username
                FROM controllers c
                LEFT JOIN users u ON u.id = c.assigned_user_id
                WHERE c.controller_type = 'HARDWARE'
                  AND c.last_seen_at > (CURRENT_TIMESTAMP - INTERVAL 1 MINUTE)
                ORDER BY c.controller_id ASC
                """).execute();
    }

    /**
     * Löscht einen Controller vollständig aus der DB (z.B. nach Hardware-Abmelden).
     */
    public Future<Void> deleteControllerByControllerId(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return Future.failedFuture("Controller-ID fehlt.");
        }
        return jdbcPool.preparedQuery("""
                DELETE FROM controllers WHERE controller_id = ?
                """).execute(Tuple.of(controllerId.trim())).mapEmpty();
    }

    /**
     * Entfernt alte FREE-Controller ohne Zuweisung (typisch geschlossene Web-Controller-Seiten).
     */
    public Future<Void> deleteStaleFreeControllers() {
        return jdbcPool.preparedQuery("""
                DELETE FROM controllers
                WHERE status = 'FREE'
                  AND assigned_user_id IS NULL
                  AND last_seen_at < (CURRENT_TIMESTAMP - INTERVAL 1 MINUTE)
                """).execute().mapEmpty();
    }
}
