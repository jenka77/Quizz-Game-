package com.example.player;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Speichert die letzte RFID-bezogene Lobby-Nachricht für Anzeige im Frontend.
 * Typ: "error" (rot), "info" (grün), "warning" (gelb, z.B. "Der Spieler ist bereits in der Session").
 */
public final class LobbyMessageService {

    private static final AtomicReference<LobbyMessage> lastMessage = new AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicLong messageSeq = new java.util.concurrent.atomic.AtomicLong(0);

    public static void setRfidMessage(String text, boolean isError) {
        setRfidMessage(text, isError ? "error" : "info");
    }

    public static void setRfidMessage(String text, String type) {
        lastMessage.set(new LobbyMessage(messageSeq.incrementAndGet(), text, type != null ? type : "info", System.currentTimeMillis()));
    }

    /**
     * Löscht die gespeicherte RFID-Nachricht (alte Fehler/Infos vergessen).
     */
    public static void clearRfidMessage() {
        lastMessage.set(null);
    }

    /**
     * Liest und löscht die letzte Nachricht (einmalige Anzeige).
     * Gibt null zurück, wenn keine Nachricht oder älter als 30 Sekunden.
     */
    public static LobbyMessage consumeLastMessage() {
        LobbyMessage msg = lastMessage.getAndSet(null);
        if (msg == null) return null;
        if (System.currentTimeMillis() - msg.timestampMs > 30_000) {
            return null;
        }
        return msg;
    }

    /**
     * Gibt die letzte Nachricht ohne sie zu löschen zurück (für Polling).
     */
    public static LobbyMessage getLastMessage() {
        LobbyMessage msg = lastMessage.get();
        if (msg == null) return null;
        if (System.currentTimeMillis() - msg.timestampMs > 30_000) {
            lastMessage.compareAndSet(msg, null);
            return null;
        }
        return msg;
    }

    public record LobbyMessage(long id, String text, String type, long timestampMs) {
        public boolean isError() { return "error".equalsIgnoreCase(type); }
    }
}
