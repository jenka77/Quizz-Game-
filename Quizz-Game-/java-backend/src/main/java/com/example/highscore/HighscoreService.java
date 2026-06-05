package com.example.highscore;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class HighscoreService {
    private final HighscoreRepository highscoreRepository;

    public HighscoreService() {
        this.highscoreRepository = new HighscoreRepository();
    }

    public Future<JsonObject> getHighscoresByQuestionCount(String questionCountRaw, int limit) {
        if (questionCountRaw == null || questionCountRaw.isBlank()) {
            return Future.failedFuture("Fragenanzahl fehlt.");
        }

        String normalized = questionCountRaw.trim();
        String roundLength;
        switch (normalized) {
            case "5" -> roundLength = "Q5";
            case "10" -> roundLength = "Q10";
            case "20" -> roundLength = "Q20";
            default -> {
                return Future.failedFuture("Ungültiger Modus. Erlaubt: 5, 10, 20.");
            }
        }

        String finalRoundLength = roundLength;
        return highscoreRepository.fetchHighscoresByRoundLength(roundLength, limit)
                .map(entries -> new JsonObject()
                        .put("questionCount", Integer.parseInt(normalized))
                        .put("roundLength", finalRoundLength)
                        .put("entries", entries));
    }
}
