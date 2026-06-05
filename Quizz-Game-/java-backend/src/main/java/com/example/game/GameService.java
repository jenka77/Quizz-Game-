package com.example.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.example.player.PlayerRepository;
import com.example.player.PlayerService;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

public class GameService {

    private static final Set<Integer> ALLOWED_QUESTION_COUNTS = Set.of(5, 10, 20);
    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameStateManager gameStateManager;

    public GameService() {
        this.gameRepository = new GameRepository();
        this.playerRepository = new PlayerRepository();
        this.gameStateManager = GameStateManager.getInstance();
    }

    public Future<JsonObject> getStatus(String controllerId) {
        JsonObject status = gameStateManager.getCurrentGame().toJson();
        if (controllerId == null || controllerId.isBlank()) {
            return Future.succeededFuture(status);
        }
        String normalizedId = controllerId.trim();
        Double score = gameStateManager.getPlayerTotalScoreByControllerId(normalizedId);
        if (score != null) {
            status.put("playerTotalScore", score);
        }
        status.put("controllerReadyState", PlayerService.getControllerReadyState(normalizedId));
        return playerRepository.findAssignedUsernameByControllerId(normalizedId)
                .map(username -> {
                    status.put("playerName", (username != null && !username.isBlank()) ? username : "");
                    return status;
                });
    }

    public Future<JsonObject> loadConfig() {
        return withCategoryStats(gameStateManager.getCurrentConfig());
    }

    public Future<JsonObject> saveConfig(JsonObject body) {
        if (body == null) {
            return Future.failedFuture("Ungültiges JSON.");
        }

        Integer questionCount = body.getInteger("questionCount");
        JsonArray categoryIdsRaw = body.getJsonArray("categoryIds", new JsonArray());
        JsonArray difficultiesRaw = body.getJsonArray("difficulties", new JsonArray());

        if (questionCount == null || !ALLOWED_QUESTION_COUNTS.contains(questionCount)) {
            return Future.failedFuture("Spielmodus muss 5, 10 oder 20 Fragen sein.");
        }

        List<Long> categoryIds = normalizeCategoryIds(categoryIdsRaw);
        if (categoryIds.isEmpty()) {
            return Future.failedFuture("Bitte mindestens eine Kategorie auswählen.");
        }

        List<String> difficulties = normalizeDifficulties(difficultiesRaw);
        if (difficulties.isEmpty()) {
            return Future.failedFuture("Bitte mindestens eine Schwierigkeit auswählen.");
        }

        return gameRepository.countMatchingQuestions(categoryIds, difficulties).compose(available -> {
            boolean valid = available >= questionCount;
            String message = valid
                    ? "Spielkonfiguration ist gültig."
                    : "Zu wenig Fragen für diese Auswahl vorhanden.";

            gameStateManager.updateConfig(questionCount, categoryIds, difficulties, available, valid, message);
            return withCategoryStats(gameStateManager.getCurrentConfig());
        });
    }

    private Future<JsonObject> withCategoryStats(JsonObject config) {
        return gameRepository.fetchCategoryDifficultyCounts().map(stats ->
                config.copy().put("categoryStats", stats));
    }

    public Future<JsonObject> startGame() {
        if (!gameStateManager.isConfigValid()) {
            return Future.failedFuture("Spielkonfiguration ist ungültig.");
        }

        return fetchActiveReadyPlayers().compose(activePlayers ->
                gameRepository.fetchRandomQuestions(
                                gameStateManager.getCategoryIds(),
                                gameStateManager.getDifficulties(),
                                gameStateManager.getQuestionCount()
                        )
                        .map(questions -> {
                            gameStateManager.startGameFlow(questions, activePlayers);
                            return gameStateManager.getCurrentGame().toJson();
                        })
        );
    }

    public Future<JsonObject> submitAnswer(String controllerId, String answer) {
        return gameStateManager.submitAnswer(controllerId, answer);
    }

    private Future<List<JsonObject>> fetchActiveReadyPlayers() {
        return playerRepository.fetchLobbyPlayers().map(rows -> {
            List<JsonObject> activePlayers = new ArrayList<>();

            for (Row row : rows) {
                String controllerStatus = row.getString("status");
                boolean connected = controllerStatus != null && controllerStatus.equalsIgnoreCase("ASSIGNED");
                if (!connected) {
                    continue;
                }

                String readyState = PlayerService.getControllerReadyState(row.getString("controller_id"));
                if (!"READY".equalsIgnoreCase(readyState)) {
                    throw new IllegalStateException("Nicht alle aktiven Spieler sind ready.");
                }

                activePlayers.add(new JsonObject()
                        .put("userId", row.getLong("user_id"))
                        .put("name", row.getString("username"))
                        .put("controllerId", row.getString("controller_id")));
            }

            if (activePlayers.isEmpty()) {
                throw new IllegalStateException("Es sind keine aktiven Spieler verbunden.");
            }

            return activePlayers;
        });
    }

    private List<Long> normalizeCategoryIds(JsonArray items) {
        Set<Long> values = new LinkedHashSet<>();
        for (Object item : items) {
            if (item instanceof Number number) {
                values.add(number.longValue());
            }
        }
        return new ArrayList<>(values);
    }

    private List<String> normalizeDifficulties(JsonArray items) {
        Set<String> values = new LinkedHashSet<>();
        for (Object item : items) {
            if (item instanceof String text) {
                String normalized = text.trim().toUpperCase();
                if (ALLOWED_DIFFICULTIES.contains(normalized)) {
                    values.add(normalized);
                }
            }
        }
        return new ArrayList<>(values);
    }
}
