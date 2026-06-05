package com.example.auth;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class AuthRepository {
    //Alle SQL-Queries in AuthRepository laufen über genau diesen einen DB-Pool.

    private final JDBCPool jdbcPool;

    public AuthRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    public Future<RowSet<Row>> findUserByUsername(String username) {
        return jdbcPool.preparedQuery("""
                SELECT id
                FROM users
                WHERE username = ?
                """).execute(Tuple.of(username));
    }

    public Future<RowSet<Row>> findUserCredentialsByUsername(String username) {
        return jdbcPool.preparedQuery("""
                SELECT id, username, password_hash, rfid_uid
                FROM users
                WHERE username = ?
                """).execute(Tuple.of(username));
    }

    public Future<RowSet<Row>> findUserByRfid(String rfidUid) {
        return jdbcPool.preparedQuery("""
                SELECT id
                FROM users
                WHERE rfid_uid = ?
                """).execute(Tuple.of(rfidUid));
    }

    /**
     * Sucht Benutzer per RFID-UID (normalisiert: Leerzeichen entfernt, Vergleich case-insensitiv).
     * Unterstützt Format "04 9C 64 D2 45 2B 80" (Arduino) und "049C64D2452B80" (DB).
     */
    public Future<RowSet<Row>> findUserByRfidNormalized(String rfidUid) {
        if (rfidUid == null || rfidUid.isBlank()) {
            return jdbcPool.preparedQuery("SELECT 1 WHERE 1=0").execute().map(r -> (RowSet<Row>) r);
        }
        String normalized = rfidUid.trim().replaceAll("\\s+", "").toUpperCase();
        return jdbcPool.preparedQuery("""
                SELECT id FROM users
                WHERE rfid_uid IS NOT NULL
                AND UPPER(REPLACE(TRIM(rfid_uid), ' ', '')) = ?
                """).execute(Tuple.of(normalized));
    }

    public Future<RowSet<Row>> hasActiveSession(Long userId) {
        return jdbcPool.preparedQuery("""
                SELECT 1 FROM active_sessions WHERE user_id = ?
                """).execute(Tuple.of(userId));
    }

    public Future<RowSet<Row>> findUserById(Long userId) {
        return jdbcPool.preparedQuery("""
                SELECT id, username, rfid_uid
                FROM users
                WHERE id = ?
                """).execute(Tuple.of(userId));
    }

    public Future<Void> insertUser(String username, String passwordHash, String rfidUid) {
        return jdbcPool.preparedQuery("""
                INSERT INTO users (username, password_hash, rfid_uid)
                VALUES (?, ?, ?)
                """).execute(Tuple.of(username, passwordHash, rfidUid)).mapEmpty();
    }

    public Future<Void> updateUserRfid(Long userId, String rfidUid) {
        return jdbcPool.preparedQuery("""
                UPDATE users
                SET rfid_uid = ?
                WHERE id = ?
                """).execute(Tuple.of(rfidUid, userId)).mapEmpty();
    }

    public Future<Void> clearUserRfid(Long userId) {
        return jdbcPool.preparedQuery("""
                UPDATE users
                SET rfid_uid = NULL
                WHERE id = ?
                """).execute(Tuple.of(userId)).mapEmpty();
    }

    public Future<Void> insertSession(Long userId, String authToken) {
        return jdbcPool.preparedQuery("""
                INSERT INTO active_sessions (user_id, auth_token)
                VALUES (?, ?)
                """).execute(Tuple.of(userId, authToken)).mapEmpty();
    }

    public Future<Void> deleteSessionByToken(String authToken) {
        return jdbcPool.preparedQuery("""
                DELETE FROM active_sessions WHERE auth_token = ?
                """).execute(Tuple.of(authToken)).mapEmpty();
    }

    public Future<Void> deleteSessionByUserId(Long userId) {
        return jdbcPool.preparedQuery("""
                DELETE FROM active_sessions WHERE user_id = ?
                """).execute(Tuple.of(userId)).mapEmpty();
    }

    public Future<java.util.Set<Long>> findAllLoggedInUserIds() {
        return jdbcPool.preparedQuery("""
                SELECT DISTINCT user_id FROM active_sessions
                """).execute().map(rows -> {
            java.util.Set<Long> ids = new java.util.HashSet<>();
            for (Row r : rows) {
                Long id = r.getLong("user_id");
                if (id != null) ids.add(id);
            }
            return ids;
        });
    }

    public Future<Void> deleteAllSessions() {
        return jdbcPool.preparedQuery("DELETE FROM active_sessions").execute().mapEmpty();
    }
}
