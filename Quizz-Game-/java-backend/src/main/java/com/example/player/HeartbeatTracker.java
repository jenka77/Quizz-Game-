package com.example.player;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks last heartbeat timestamp per controller.
 * Used for: Heartbeat every 10s, 2 missed = disconnected (ProjektBeschreibung).
 */
public final class HeartbeatTracker {

    private static final HeartbeatTracker INSTANCE = new HeartbeatTracker();

    private final ConcurrentHashMap<String, Long> lastHeartbeatAtMs = new ConcurrentHashMap<>();

    private HeartbeatTracker() {
    }

    public static HeartbeatTracker getInstance() {
        return INSTANCE;
    }

    public void record(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return;
        }
        lastHeartbeatAtMs.put(controllerId.trim().toUpperCase(), System.currentTimeMillis());
    }

    public void remove(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return;
        }
        lastHeartbeatAtMs.remove(controllerId.trim().toUpperCase());
    }

    public boolean isStale(String controllerId, long maxAgeMs) {
        if (controllerId == null || controllerId.isBlank()) {
            return true;
        }
        Long last = lastHeartbeatAtMs.get(controllerId.trim().toUpperCase());
        if (last == null) {
            return true;
        }
        return System.currentTimeMillis() - last > maxAgeMs;
    }
}
