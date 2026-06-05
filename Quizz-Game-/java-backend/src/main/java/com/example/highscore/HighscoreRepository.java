package com.example.highscore;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class HighscoreRepository {
    private final JDBCPool jdbcPool;

    public HighscoreRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    public Future<JsonArray> fetchHighscoresByRoundLength(String roundLength, int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        return jdbcPool.preparedQuery("""
                SELECT
                    h.id,
                    h.round_length,
                    h.user_id,
                    u.username,
                    h.total_points,
                    h.created_at
                FROM highscores h
                JOIN users u ON u.id = h.user_id
                WHERE h.round_length = ?
                ORDER BY h.total_points DESC, h.created_at ASC, h.id ASC
                LIMIT ?
                """)
                .execute(Tuple.of(roundLength, safeLimit))
                .map(rows -> {
                    JsonArray highscores = new JsonArray();
                    int rank = 1;
                    for (Row row : rows) {
                        highscores.add(new JsonObject()
                                .put("rank", rank++)
                                .put("id", row.getLong("id"))
                                .put("roundLength", row.getString("round_length"))
                                .put("userId", row.getLong("user_id"))
                                .put("username", row.getString("username"))
                                .put("points", row.getDouble("total_points"))
                                .put("createdAt", formatTimestampAsUtcIso(row.getValue("created_at"))));
                    }
                    return highscores;
                });
    }

    /** Konvertiert den Timestamp in ISO 8601 UTC (z.B. 2025-03-08T14:30:00Z) für korrekte Anzeige in lokaler Zeit. */
    private static String formatTimestampAsUtcIso(Object value) {
        if (value == null) return null;
        Instant instant = null;
        if (value instanceof java.sql.Timestamp ts) {
            instant = ts.toInstant();
        } else if (value instanceof LocalDateTime ldt) {
            instant = ldt.toInstant(ZoneOffset.UTC);
        } else if (value instanceof java.util.Date d) {
            instant = d.toInstant();
        }
        return instant != null ? instant.toString() : value.toString();
    }
}
