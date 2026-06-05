package com.example.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.example.player.PlayerRepository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

public class AuthService {
    // Einfache In-Memory-Sessionverwaltung (2. Semester Niveau).
    private static final Map<String, Long> sessionsByToken = new ConcurrentHashMap<>();
    private static final Map<Long, String> sessionTokenByUserId = new ConcurrentHashMap<>();
    private static final SecureRandom secureRandom = new SecureRandom();

    private final AuthRepository authRepository;
    private final PlayerRepository playerRepository;

    public AuthService() {
        this.authRepository = new AuthRepository();
        this.playerRepository = new PlayerRepository();
    }

    public Future<JsonObject> register(String username, String password, String rfidUid) {
        if (username == null || username.isBlank()) {
            return Future.failedFuture("Benutzername fehlt.");
        }

        if (password == null || password.length() < 8) {
            return Future.failedFuture("Passwort muss mindestens 8 Zeichen haben.");
        }

        String normalizedUsername = username.trim();
        String normalizedRfid = normalizeRfid(rfidUid);
        String passwordHash = sha256(password);

        return authRepository.findUserByUsername(normalizedUsername).compose(userRows -> {
            if (userRows.size() > 0) {
                return Future.failedFuture("Benutzername ist bereits vergeben.");
            }

            if (normalizedRfid == null) {
                return authRepository.insertUser(normalizedUsername, passwordHash, null)
                        .map(new JsonObject().put("message", "Registrierung erfolgreich."));
            }

            return authRepository.findUserByRfidNormalized(normalizedRfid).compose(rfidRows -> {
                if (rfidRows.size() > 0) {
                    return Future.failedFuture("RFID ist bereits vergeben.");
                }

                return authRepository.insertUser(normalizedUsername, passwordHash, normalizedRfid)
                        .map(new JsonObject().put("message", "Registrierung erfolgreich."));
            });
        });
    }

    public Future<JsonObject> login(String username, String password, String rfidUid) {
        if (username == null || username.isBlank()) {
            return Future.failedFuture("Benutzername fehlt.");
        }

        if (password == null || password.isBlank()) {
            return Future.failedFuture("Passwort fehlt.");
        }

        String normalizedUsername = username.trim();
        String passwordHash = sha256(password);
        String normalizedRfid = normalizeRfid(rfidUid);

        // Login immer über Benutzername (nicht Passwort). Mehrere Spieler können dasselbe Passwort haben.
        return authRepository.findUserCredentialsByUsername(normalizedUsername).compose(userRows -> {
            if (userRows.size() == 0) {
                return Future.failedFuture("Ungültiger Benutzername oder Passwort.");
            }

            Row row = userRows.iterator().next();
            String storedHash = row.getString("password_hash");
            if (storedHash == null || !storedHash.equals(passwordHash)) {
                return Future.failedFuture("Ungültiger Benutzername oder Passwort.");
            }

            Long userId = row.getLong("id");
            String rowUsername = row.getString("username");
            String currentRfidUid = row.getString("rfid_uid");

            if (normalizedRfid == null) {
                return createSessionToken(userId).map(token -> new JsonObject()
                        .put("message", "Login erfolgreich.")
                        .put("authToken", token)
                        .put("userId", userId)
                        .put("username", rowUsername)
                        .put("rfidUid", currentRfidUid));
            }

            return authRepository.findUserByRfidNormalized(normalizedRfid).compose(rfidRows -> {
                if (rfidRows.size() > 0) {
                    Long ownerId = rfidRows.iterator().next().getLong("id");
                    if (ownerId != null && !ownerId.equals(userId)) {
                        return Future.failedFuture("RFID ist bereits vergeben.");
                    }
                }

                String currentCanonical = normalizeRfid(currentRfidUid);
                Future<Void> updateFuture = normalizedRfid.equals(currentCanonical)
                        ? Future.succeededFuture()
                        : authRepository.updateUserRfid(userId, normalizedRfid);

                return updateFuture.compose(v ->
                        createSessionToken(userId).map(token -> new JsonObject()
                                .put("message", "Login erfolgreich.")
                                .put("authToken", token)
                                .put("userId", userId)
                                .put("username", rowUsername)
                                .put("rfidUid", normalizedRfid))
                );
            });
        });
    }

    /**
     * Ergebnis der RFID-Verarbeitung vom Hardware (MQTT).
     */
    public enum RfidHardwareResult {
        NOT_FOUND,           // RFID nicht erkannt
        ALREADY_IN_SESSION,  // Der Spieler ist bereits in der Session
        OK                   // Login + Bind erfolgreich
    }

    /**
     * Verarbeitet RFID-Scan vom Hardware: sucht Benutzer, prüft Session,
     * erstellt Session bei Bedarf. Nutzt normalisierte Suche (Leerzeichen ignoriert).
     */
    public Future<RfidHardwareOutcome> handleRfidFromHardware(String rfidUid) {
        if (rfidUid == null || rfidUid.isBlank()) {
            return Future.succeededFuture(new RfidHardwareOutcome(RfidHardwareResult.NOT_FOUND, null, null, null));
        }

        return authRepository.findUserByRfidNormalized(rfidUid).compose(rfidRows -> {
            if (rfidRows.size() == 0) {
                return Future.succeededFuture(new RfidHardwareOutcome(RfidHardwareResult.NOT_FOUND, null, null, null));
            }
            Long userId = rfidRows.iterator().next().getLong("id");
            if (userId == null) {
                return Future.succeededFuture(new RfidHardwareOutcome(RfidHardwareResult.NOT_FOUND, null, null, null));
            }

            return authRepository.hasActiveSession(userId).compose(sessionRows -> {
                if (sessionRows.size() > 0) {
                    return Future.succeededFuture(new RfidHardwareOutcome(RfidHardwareResult.ALREADY_IN_SESSION, userId, null, null));
                }

                return authRepository.findUserById(userId).compose(userRows -> {
                    if (userRows.size() == 0) {
                        return Future.succeededFuture(new RfidHardwareOutcome(RfidHardwareResult.NOT_FOUND, null, null, null));
                    }
                    String username = userRows.iterator().next().getString("username");

                    return createSessionToken(userId).map(token ->
                            new RfidHardwareOutcome(RfidHardwareResult.OK, userId, username, token));
                });
            });
        });
    }

    public record RfidHardwareOutcome(RfidHardwareResult result, Long userId, String username, String authToken) {}

    public Future<JsonObject> loginRfid(String rfidUid) {
        if (rfidUid == null || rfidUid.isBlank()) {
            return Future.failedFuture("RFID fehlt.");
        }

        String normalizedRfid = normalizeRfid(rfidUid);
        if (normalizedRfid == null) {
            return Future.failedFuture("RFID fehlt.");
        }

        return authRepository.findUserByRfidNormalized(normalizedRfid).compose(rfidRows -> {
            if (rfidRows.size() == 0) {
                return Future.failedFuture("RFID ist nicht bekannt.");
            }
            Long userId = rfidRows.iterator().next().getLong("id");
            if (userId == null) {
                return Future.failedFuture("RFID ist nicht bekannt.");
            }

            return authRepository.findUserById(userId).compose(userRows -> {
                if (userRows.size() == 0) {
                    return Future.failedFuture("Benutzer nicht gefunden.");
                }
                String username = userRows.iterator().next().getString("username");

                return createSessionToken(userId).map(token -> new JsonObject()
                        .put("message", "Login erfolgreich.")
                        .put("authToken", token)
                        .put("userId", userId)
                        .put("username", username)
                        .put("rfidUid", normalizedRfid));
            });
        });
    }

    public Future<JsonObject> setRfid(String authToken, String rfidUid) {
        Long userId = resolveUserId(authToken);
        if (userId == null) {
            return Future.failedFuture("Nicht eingeloggt.");
        }

        String normalizedRfid = normalizeRfid(rfidUid);
        if (normalizedRfid == null) {
            return Future.failedFuture("RFID fehlt.");
        }

        return authRepository.findUserById(userId).compose(userRows -> {
            if (userRows.size() == 0) {
                return Future.failedFuture("Benutzer nicht gefunden.");
            }

            Row userRow = userRows.iterator().next();
            String username = userRow.getString("username");

            return authRepository.findUserByRfidNormalized(normalizedRfid).compose(rfidRows -> {
                if (rfidRows.size() > 0) {
                    Long ownerId = rfidRows.iterator().next().getLong("id");
                    if (ownerId != null && !ownerId.equals(userId)) {
                        return Future.failedFuture("RFID ist bereits vergeben.");
                    }
                }

                return authRepository.updateUserRfid(userId, normalizedRfid)
                        .map(new JsonObject()
                                .put("message", "RFID erfolgreich gesetzt.")
                                .put("userId", userId)
                                .put("username", username)
                                .put("rfidUid", normalizedRfid));
            });
        });
    }

    public Future<JsonObject> removeRfid(String authToken) {
        Long userId = resolveUserId(authToken);
        if (userId == null) {
            return Future.failedFuture("Nicht eingeloggt.");
        }

        return authRepository.findUserById(userId).compose(userRows -> {
            if (userRows.size() == 0) {
                return Future.failedFuture("Benutzer nicht gefunden.");
            }

            Row userRow = userRows.iterator().next();
            String username = userRow.getString("username");

            return authRepository.clearUserRfid(userId)
                    .map(new JsonObject()
                            .put("message", "RFID erfolgreich entfernt.")
                            .put("userId", userId)
                            .put("username", username)
                            .put("rfidUid", (String) null));
        });
    }

    /**
     * Meldet nur diesen einen Benutzer ab. Die Lobby-Session und alle anderen Spieler bleiben unverändert.
     * Mehrere Nutzer können gleichzeitig eingeloggt sein und gemeinsam spielen.
     */
    public Future<JsonObject> logout(String authToken) {
        Long removed = null;
        if (authToken != null && !authToken.isBlank()) {
            String normalizedToken = authToken.trim();
            removed = sessionsByToken.remove(normalizedToken);
            if (removed != null) {
                sessionTokenByUserId.remove(removed);
            }
        }

        if (removed == null) {
            return Future.failedFuture("Nicht eingeloggt.");
        }
        final Long userIdToClear = removed;

        authRepository.deleteSessionByToken(authToken.trim()).onFailure(e -> { /* log */ });

        // Nur die Controller dieses einen Benutzers freigeben; andere Spieler unberührt.
        // Controller-IDs vor dem Löschen holen, damit der Aufrufer den In-Memory-State bereinigen kann.
        return playerRepository.fetchControllersByUserId(userIdToClear)
                .compose(rows -> {
                    List<String> controllerIds = new ArrayList<>();
                    for (Row row : rows) {
                        String cid = row.getString("controller_id");
                        if (cid != null && !cid.isBlank()) {
                            controllerIds.add(cid.trim());
                        }
                    }
                    return playerRepository.clearAllControllerAssignmentsForUser(userIdToClear)
                            .map(v -> new JsonObject()
                                    .put("message", "Logout erfolgreich.")
                                    .put("controllerIds", new JsonArray(controllerIds)));
                });
    }

    private Long resolveUserId(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return null;
        }
        return sessionsByToken.get(authToken.trim());
    }

    public Long resolveUserIdForToken(String authToken) {
        return resolveUserId(authToken);
    }

    /**
     * Liefert die aktuellen Benutzerdaten (username, rfidUid) für den eingeloggten Benutzer.
     * rfidUid ist null wenn der Benutzer keine RFID-Karte hat.
     */
    public Future<JsonObject> getCurrentUser(String authToken) {
        Long userId = resolveUserId(authToken);
        if (userId == null) {
            return Future.failedFuture("Nicht eingeloggt.");
        }
        return authRepository.findUserById(userId)
                .map(rows -> {
                    if (rows.size() == 0) {
                        return new JsonObject().put("userId", userId).put("username", "").put("rfidUid", (String) null);
                    }
                    Row row = rows.iterator().next();
                    return new JsonObject()
                            .put("userId", row.getLong("id"))
                            .put("username", row.getString("username"))
                            .put("rfidUid", row.getString("rfid_uid"));
                });
    }

    public Future<Set<Long>> getLoggedInUserIdsSnapshot() {
        return authRepository.findAllLoggedInUserIds().map(ids -> (Set<Long>) new HashSet<>(ids));
    }

    public boolean invalidateSessionForUser(Long userId) {
        if (userId == null) {
            return false;
        }

        String oldToken = sessionTokenByUserId.remove(userId);
        if (oldToken == null) {
            return false;
        }

        sessionsByToken.remove(oldToken);
        authRepository.deleteSessionByToken(oldToken).onFailure(e -> {});
        return true;
    }

    /**
     * Beendet ALLE Spielersessions und entfernt alle Controller (auch freie).
     * Gibt die Liste der Controller-IDs zurück, damit Fenster geschlossen und In-Memory-State bereinigt werden können.
     */
    public Future<JsonObject> resetAllSessionsAndControllers() {
        return playerRepository.fetchAllControllerIds()
                .compose(controllerIds -> {
                    sessionsByToken.clear();
                    sessionTokenByUserId.clear();
                    return authRepository.deleteAllSessions()
                            .compose(v -> playerRepository.deleteAllControllers().map(x -> controllerIds));
                })
                .map(controllerIds -> new JsonObject()
                        .put("message", "Alle Sessions und Controller wurden zurückgesetzt.")
                        .put("controllerIds", new JsonArray(controllerIds)));
    }

    private Future<String> createSessionToken(Long userId) {
        String oldToken = sessionTokenByUserId.remove(userId);
        if (oldToken != null) {
            sessionsByToken.remove(oldToken);
            authRepository.deleteSessionByUserId(userId).onFailure(e -> {});
        }

        byte[] raw = new byte[24];
        secureRandom.nextBytes(raw);
        String token = HexFormat.of().formatHex(raw);
        sessionsByToken.put(token, userId);
        sessionTokenByUserId.put(userId, token);
        return authRepository.insertSession(userId, token).map(v -> token);
    }

    /**
     * Normalisiert RFID-UID für Speicherung und Vergleich.
     * Entfernt Leerzeichen, Großbuchstaben – "04 9C 64" und "049c64" werden identisch behandelt.
     */
    private String normalizeRfid(String rfidUid) {
        if (rfidUid == null) {
            return null;
        }
        String canonical = rfidUid.trim().replaceAll("\\s+", "").toUpperCase();
        return canonical.isEmpty() ? null : canonical;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }
}
