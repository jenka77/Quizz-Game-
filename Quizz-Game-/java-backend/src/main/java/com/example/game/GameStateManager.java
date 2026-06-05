package com.example.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GameStateManager {

    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);

    private static GameStateManager instance;

    private final Vertx vertx;
    private final EventBus eventBus;
    private final GameRepository gameRepository;
    private GameModel currentGame;
    private Long countdownTimerId;
    private Long questionTimerId;
    private Long evaluationTimerId;
    private Long preQuestionTimerId;
    private String currentPreQuestionPingId;
    private final Set<String> preQuestionRespondedControllerIds = new HashSet<>();
    private int questionCount = 5;
    private List<Long> categoryIds = new ArrayList<>();
    private List<String> difficulties = new ArrayList<>(List.of("EASY", "MEDIUM"));
    private int availableQuestionCount = 0;
    private boolean configValid = false;
    private String configMessage = "Bitte Kategorien auswählen.";
    private final List<JsonObject> plannedQuestions = new ArrayList<>();
    private int currentQuestionPointer = -1;
    private final Map<Long, String> gamePlayerNames = new LinkedHashMap<>();
    private final Map<Long, String> gamePlayerControllerIds = new LinkedHashMap<>();
    private final Map<String, Long> controllerToUserIds = new HashMap<>();
    private final Map<Long, Double> gameTotalScores = new LinkedHashMap<>();
    private final Set<Long> activeResponderUserIds = new HashSet<>();
    private final Set<Long> disconnectedUserIds = new HashSet<>();
    private final Map<Long, JsonObject> currentQuestionAnswers = new LinkedHashMap<>();
    private long currentQuestionStartedAtMs = 0L;

    private GameStateManager(Vertx vertx) {
        this.vertx = vertx;
        this.eventBus = vertx.eventBus();
        this.gameRepository = new GameRepository();
        this.currentGame = new GameModel();
    }

    public static synchronized GameStateManager getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new GameStateManager(vertx);
        }
        return instance;
    }

    public static synchronized GameStateManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GameStateManager ist noch nicht initialisiert.");
        }
        return instance;
    }

    public GameModel getCurrentGame() {
        return currentGame;
    }

    public Double getPlayerTotalScoreByControllerId(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return null;
        }
        Long userId = controllerToUserIds.get(controllerId.trim().toUpperCase(Locale.ROOT));
        if (userId == null) {
            return null;
        }
        return gameTotalScores.get(userId);
    }

    public boolean isCountdownRunning() {
        return countdownTimerId != null;
    }

    public JsonObject getCurrentConfig() {
        return new JsonObject()
                .put("questionCount", questionCount)
                .put("categoryIds", new JsonArray(categoryIds))
                .put("difficulties", new JsonArray(difficulties))
                .put("availableQuestionCount", availableQuestionCount)
                .put("valid", configValid)
                .put("message", configMessage);
    }

    public void updateConfig(
            int questionCount,
            List<Long> categoryIds,
            List<String> difficulties,
            int availableQuestionCount,
            boolean configValid,
            String configMessage
    ) {
        this.questionCount = questionCount;
        this.categoryIds = new ArrayList<>(categoryIds);
        this.difficulties = new ArrayList<>(difficulties);
        this.availableQuestionCount = availableQuestionCount;
        this.configValid = configValid;
        this.configMessage = configMessage;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public List<Long> getCategoryIds() {
        return new ArrayList<>(categoryIds);
    }

    public List<String> getDifficulties() {
        return new ArrayList<>(difficulties);
    }

    public boolean isConfigValid() {
        return configValid;
    }

    public void startCountdown(int seconds, Runnable onFinished) {
        cancelCountdown(null);
        cancelQuestionTimer();
        cancelEvaluationTimer();

        currentGame = new GameModel();
        currentGame.setState("COUNTDOWN");
        currentGame.setCountdownSeconds(seconds);
        currentGame.setTotalQuestions(questionCount);
        currentGame.setMessage("Spiel startet in " + seconds + " Sekunden.");
        publishGameStateChanged();
        eventBus.publish("game.countdown.tick", seconds);

        countdownTimerId = vertx.setPeriodic(1000, timerId -> {
            Integer remaining = currentGame.getCountdownSeconds() == null ? 0 : currentGame.getCountdownSeconds() - 1;

            if (remaining <= 0) {
                vertx.cancelTimer(timerId);
                countdownTimerId = null;
                onFinished.run();
                return;
            }

            currentGame.setCountdownSeconds(remaining);
            currentGame.setMessage("Spiel startet in " + remaining + " Sekunden.");
            publishGameStateChanged();
            eventBus.publish("game.countdown.tick", remaining);
        });
    }

    public void cancelCountdown(String message) {
        if (countdownTimerId != null) {
            vertx.cancelTimer(countdownTimerId);
            countdownTimerId = null;
        }

        if (message != null) {
            resetToLobby(message);
        }
    }

    public void resetToLobby(String message) {
        cancelCountdown(null);
        cancelPreQuestionTimer();
        cancelQuestionTimer();
        cancelEvaluationTimer();
        plannedQuestions.clear();
        currentQuestionPointer = -1;
        gamePlayerNames.clear();
        gamePlayerControllerIds.clear();
        controllerToUserIds.clear();
        gameTotalScores.clear();
        activeResponderUserIds.clear();
        disconnectedUserIds.clear();
        currentQuestionAnswers.clear();
        currentQuestionStartedAtMs = 0L;
        currentGame = new GameModel();
        currentGame.setTotalQuestions(questionCount);
        currentGame.setMessage(message == null ? "Warte auf Spielstart." : message);
        publishGameStateChanged();
    }

    public void startGameFlow(List<JsonObject> questions, List<JsonObject> players) {
        if (questions == null || questions.isEmpty()) {
            resetToLobby("Keine passenden Fragen gefunden.");
            return;
        }
        if (players == null || players.isEmpty()) {
            resetToLobby("Keine aktiven Spieler für den Spielstart gefunden.");
            return;
        }

        plannedQuestions.clear();
        plannedQuestions.addAll(questions);
        currentQuestionPointer = -1;
        questionCount = questions.size();
        gamePlayerNames.clear();
        gamePlayerControllerIds.clear();
        controllerToUserIds.clear();
        gameTotalScores.clear();
        activeResponderUserIds.clear();
        disconnectedUserIds.clear();
        currentQuestionAnswers.clear();
        for (JsonObject player : players) {
            Long userId = player.getLong("userId");
            if (userId == null) {
                continue;
            }
            String name = player.getString("name", "Spieler " + userId);
            String controllerId = player.getString("controllerId", "").trim();
            gamePlayerNames.put(userId, name);
            gamePlayerControllerIds.put(userId, controllerId);
            if (!controllerId.isBlank()) {
                String key = controllerId.toUpperCase(Locale.ROOT).trim();
                controllerToUserIds.put(key, userId);
                logger.info("Game started: player {} (controller '{}') added to game.", name, key);
            }
            gameTotalScores.put(userId, 0.0);
            activeResponderUserIds.add(userId);
        }

        eventBus.publish("game.start", null);

        // Status sofort an alle Controller pushen (Hardware erhält Namen + Status ohne Verzögerung)
        pushStatusToGameControllers();

        // Ping und Countdown starten und enden gleichzeitig (3 s)
        startPreQuestionPhase(0);
    }

    private void startPreQuestionPhase(int index) {
        if (index < 0 || index >= plannedQuestions.size()) {
            resetToLobby("Spiel beendet.");
            return;
        }

        currentPreQuestionPingId = java.util.UUID.randomUUID().toString();
        preQuestionRespondedControllerIds.clear();

        currentGame.setState("PRE_QUESTION");
        currentGame.setCountdownSeconds(3);
        currentGame.setPreQuestionPingId(currentPreQuestionPingId);
        currentGame.setMessage("Pre-Question-Ping: Bitte innerhalb von 3 Sekunden antworten.");
        currentGame.setCurrentQuestion(null);
        currentGame.setEvaluationPlayers(new JsonArray());
        publishGameStateChanged();
        eventBus.publish("game.countdown.tick", 3);
        vertx.setTimer(1000, id -> eventBus.publish("game.countdown.tick", 2));
        vertx.setTimer(2000, id -> eventBus.publish("game.countdown.tick", 1));

        cancelPreQuestionTimer();
        JsonArray controllerIds = new JsonArray();
        for (String cid : gamePlayerControllerIds.values()) {
            if (cid != null && !cid.isBlank()) {
                controllerIds.add(cid);
            }
        }
        eventBus.publish("game.pre-question-ping", new JsonObject()
                .put("pingId", currentPreQuestionPingId)
                .put("controllerIds", controllerIds));

        preQuestionTimerId = vertx.setTimer(3000, tid -> {
            preQuestionTimerId = null;
            // Ping/Pong nicht zum Disconnect verwenden: Web + Hardware können MQTT-Latenz haben.
            // Hardware: Arduino loop + WiFi können 3s überschreiten. Niemand wird für fehlendes Pong gekickt.
            startQuestion(index);
        });
    }

    public boolean recordPreQuestionPong(String controllerId, String pingId) {
        if (controllerId == null || controllerId.isBlank() || pingId == null || pingId.isBlank()) {
            return false;
        }
        if (!pingId.equals(currentPreQuestionPingId)) {
            return false;
        }
        if (!"PRE_QUESTION".equalsIgnoreCase(currentGame.getState())
                && !"EVALUATION".equalsIgnoreCase(currentGame.getState())) {
            return false;
        }
        preQuestionRespondedControllerIds.add(controllerId.trim().toUpperCase(Locale.ROOT));
        return true;
    }

    private void cancelPreQuestionTimer() {
        if (preQuestionTimerId != null) {
            vertx.cancelTimer(preQuestionTimerId);
            preQuestionTimerId = null;
        }
        currentPreQuestionPingId = null;
    }

    /** Publiziert State-Übergang an MQTT game/state (DesignVorschlag). */
    private void publishGameStateChanged() {
        JsonObject payload = new JsonObject()
                .put("state", currentGame.getState())
                .put("countdownSeconds", currentGame.getCountdownSeconds())
                .put("message", currentGame.getMessage());
        eventBus.publish("game.state.changed", payload);
    }

    /**
     * Publiziert ein Event damit MqttController sofort den Status (playerName, state, etc.) sendet
     * an alle Controller im Spiel (Hardware + Web).
     */
    private void pushStatusToGameControllers() {
        JsonArray controllerIds = new JsonArray();
        for (String cid : gamePlayerControllerIds.values()) {
            if (cid != null && !cid.isBlank()) {
                controllerIds.add(cid);
            }
        }
        if (!controllerIds.isEmpty()) {
            eventBus.publish("game.status.push.controllers", new JsonObject().put("controllerIds", controllerIds));
        }
    }

    private void startQuestion(int index) {
        if (index < 0 || index >= plannedQuestions.size()) {
            resetToLobby("Spiel beendet.");
            return;
        }

        currentQuestionPointer = index;
        JsonObject question = plannedQuestions.get(index);
        currentQuestionAnswers.clear();
        currentQuestionStartedAtMs = System.currentTimeMillis();

        currentGame.setPreQuestionPingId(null);
        currentGame.setState("QUESTION");
        currentGame.setCountdownSeconds(30);
        currentGame.setCurrentQuestionIndex(index + 1);
        currentGame.setTotalQuestions(questionCount);
        currentGame.setCurrentQuestion(toPublicQuestion(question));
        currentGame.setMessage("Frage " + (index + 1) + " läuft. Noch 30 Sekunden.");
        currentGame.setEvaluationPlayers(new JsonArray());
        publishGameStateChanged();

        // Status (state=QUESTION) sofort an alle Controller pushen – Hardware kann sofort antworten
        pushStatusToGameControllers();

        cancelQuestionTimer();
        // Timer 30 s: Auswertung nach Ablauf, außer alle haben vorher geantwortet (tryFinishQuestionEarly)
        questionTimerId = vertx.setPeriodic(1000, timerId -> {
            Integer remaining = currentGame.getCountdownSeconds() == null ? 0 : currentGame.getCountdownSeconds() - 1;
            if (remaining <= 0) {
                currentGame.setCountdownSeconds(0);
                currentGame.setMessage("Zeit abgelaufen.");
                vertx.cancelTimer(timerId);
                questionTimerId = null;
                startEvaluationPhase(question);
                return;
            }

            currentGame.setCountdownSeconds(remaining);
            currentGame.setMessage("Frage " + (index + 1) + " läuft. Noch " + remaining + " Sekunden.");
        });

        eventBus.publish("game.question", toPublicQuestion(question));
    }

    public io.vertx.core.Future<JsonObject> submitAnswer(String controllerId, String answer) {
        if (controllerId == null || controllerId.isBlank()) {
            return io.vertx.core.Future.failedFuture("Controller-ID fehlt.");
        }
        if (answer == null || answer.isBlank()) {
            return io.vertx.core.Future.failedFuture("Antwort fehlt.");
        }
        if (!"QUESTION".equalsIgnoreCase(currentGame.getState())) {
            return io.vertx.core.Future.failedFuture("Aktuell läuft keine Frage.");
        }
        if (currentQuestionPointer < 0 || currentQuestionPointer >= plannedQuestions.size()) {
            return io.vertx.core.Future.failedFuture("Keine aktive Frage gefunden.");
        }

        String normalizedControllerId = controllerId.trim().toUpperCase(Locale.ROOT);
        Long userId = controllerToUserIds.get(normalizedControllerId);
        if (userId == null) {
            for (java.util.Map.Entry<String, Long> e : controllerToUserIds.entrySet()) {
                if (e.getKey() != null && e.getKey().trim().equalsIgnoreCase(normalizedControllerId)) {
                    userId = e.getValue();
                    break;
                }
            }
        }
        if (userId == null) {
            for (java.util.Map.Entry<Long, String> e : gamePlayerControllerIds.entrySet()) {
                if (e.getValue() != null && e.getValue().trim().equalsIgnoreCase(normalizedControllerId)) {
                    userId = e.getKey();
                    break;
                }
            }
        }
        if (userId == null) {
            logger.warn("Answer rejected: controller '{}' not in game. In game: {}", normalizedControllerId, controllerToUserIds.keySet());
            return io.vertx.core.Future.failedFuture("Controller ist keinem aktiven Spieler zugeordnet.");
        }
        if (currentQuestionAnswers.containsKey(userId)) {
            return io.vertx.core.Future.failedFuture("Antwort wurde bereits abgegeben.");
        }

        String normalizedAnswer = answer.trim().toUpperCase(Locale.ROOT);
        if (!List.of("A", "B", "C", "D").contains(normalizedAnswer)) {
            return io.vertx.core.Future.failedFuture("Ungültige Antwort. Erlaubt: A, B, C, D.");
        }

        JsonObject fullQuestion = plannedQuestions.get(currentQuestionPointer);
        String correctOption = fullQuestion.getString("correctOption", "").toUpperCase(Locale.ROOT);
        String difficulty = fullQuestion.getString("difficulty", "EASY").toUpperCase(Locale.ROOT);

        long elapsedMs = Math.max(0L, System.currentTimeMillis() - currentQuestionStartedAtMs);
        boolean withinTime = elapsedMs < 30000 && currentGame.getCountdownSeconds() != null && currentGame.getCountdownSeconds() > 0;
        boolean isCorrect = withinTime && normalizedAnswer.equals(correctOption);
        double awardedPoints = isCorrect ? basePointsForDifficulty(difficulty) * timeFactor(elapsedMs) : 0.0;

        double updatedTotal = gameTotalScores.getOrDefault(userId, 0.0) + awardedPoints;
        gameTotalScores.put(userId, updatedTotal);

        JsonObject answerResult = new JsonObject()
                .put("userId", userId)
                .put("name", gamePlayerNames.getOrDefault(userId, "Spieler " + userId))
                .put("controllerId", gamePlayerControllerIds.getOrDefault(userId, "-"))
                .put("selectedOption", normalizedAnswer)
                .put("correctOption", correctOption)
                .put("isCorrect", isCorrect)
                .put("responseTimeMs", elapsedMs)
                .put("points", awardedPoints);
        currentQuestionAnswers.put(userId, answerResult);

        tryFinishQuestionEarly(fullQuestion);

        return io.vertx.core.Future.succeededFuture(new JsonObject()
                .put("accepted", true)
                .put("isCorrect", isCorrect)
                .put("points", awardedPoints)
                .put("totalPoints", updatedTotal));
    }

    private void startEvaluationPhase(JsonObject fullQuestion) {
        if (!"QUESTION".equalsIgnoreCase(currentGame.getState())) {
            return;
        }

        currentGame.setState("EVALUATION");
        currentGame.setCountdownSeconds(3);
        boolean allDisconnected = !hasActivePlayers();
        currentGame.setMessage(allDisconnected
                ? "Alle Spieler haben das Quiz verlassen. Zwischenstand wird angezeigt."
                : "Auswertung läuft. Noch 3 Sekunden.");
        currentGame.setCurrentQuestion(null);
        currentGame.setGameAborted(allDisconnected);
        publishGameStateChanged();
        eventBus.publish("game.countdown.tick", 3);

        buildEvaluationPlayers(fullQuestion);

        // Pre-Question-Ping während Auswertung: Ping und Pong laufen parallel zu den 3 Sekunden
        int nextIndex = currentQuestionPointer + 1;
        if (hasActivePlayers() && nextIndex < plannedQuestions.size()) {
            currentPreQuestionPingId = java.util.UUID.randomUUID().toString();
            preQuestionRespondedControllerIds.clear();
            currentGame.setPreQuestionPingId(currentPreQuestionPingId);
            JsonArray controllerIds = new JsonArray();
            for (String cid : gamePlayerControllerIds.values()) {
                if (cid != null && !cid.isBlank()) {
                    controllerIds.add(cid);
                }
            }
            eventBus.publish("game.pre-question-ping", new JsonObject()
                    .put("pingId", currentPreQuestionPingId)
                    .put("controllerIds", controllerIds));
        }

        cancelEvaluationTimer();
        evaluationTimerId = vertx.setPeriodic(1000, timerId -> {
            Integer remaining = currentGame.getCountdownSeconds() == null ? 0 : currentGame.getCountdownSeconds() - 1;
            if (remaining <= 0) {
                vertx.cancelTimer(timerId);
                evaluationTimerId = null;
                if (!hasActivePlayers()) {
                    startEndPhase();
                } else if (nextIndex < plannedQuestions.size()) {
                    // Pong-Auswertung: Non-Responder trennen, dann nächste Frage
                    processPreQuestionNonRespondersAndStartQuestion(nextIndex);
                } else {
                    startEndPhase();
                }
                return;
            }

            currentGame.setCountdownSeconds(remaining);
            currentGame.setMessage("Auswertung läuft. Noch " + remaining + " Sekunden.");
            publishGameStateChanged();
            eventBus.publish("game.countdown.tick", remaining);
        });
    }

    private void processPreQuestionNonRespondersAndStartQuestion(int index) {
        // Ping/Pong nicht zum Disconnect: Hardware/Web können MQTT-Latenz haben – niemand kicken
        currentPreQuestionPingId = null;
        currentGame.setPreQuestionPingId(null);
        startQuestion(index);
    }

    private void buildEvaluationPlayers(JsonObject fullQuestion) {
        JsonArray entries = new JsonArray();
        String correctOption = fullQuestion.getString("correctOption", "-");
        for (Map.Entry<Long, String> player : gamePlayerNames.entrySet()) {
            JsonObject answerResult = currentQuestionAnswers.get(player.getKey());
            String resultLabel = disconnectedUserIds.contains(player.getKey()) ? "Disconnected" : "Timeout";
            String pointsLabel = "0 Punkte";
            String selectedOption = "-";

            if (answerResult != null) {
                boolean isCorrect = answerResult.getBoolean("isCorrect", false);
                resultLabel = isCorrect ? "Richtig" : "Falsch";
                pointsLabel = formatScore(answerResult.getDouble("points", 0.0));
                selectedOption = answerResult.getString("selectedOption", "-");
            }

            double totalScore = gameTotalScores.getOrDefault(player.getKey(), 0.0);
            entries.add(new JsonObject()
                    .put("userId", player.getKey())
                    .put("name", player.getValue())
                    .put("controllerId", gamePlayerControllerIds.getOrDefault(player.getKey(), "-"))
                    .put("selectedOption", selectedOption)
                    .put("correctOption", correctOption)
                    .put("result", resultLabel)
                    .put("points", pointsLabel)
                    .put("totalScore", totalScore)
                    .put("totalScoreLabel", formatScore(totalScore)));
        }
        currentGame.setEvaluationPlayers(entries);
        currentQuestionAnswers.clear();
    }

    /**
     * Gibt an, ob noch mindestens ein Spieler aktiv (nicht disconnected) ist.
     * Wenn false, soll das Spiel beendet werden.
     */
    public boolean hasActivePlayers() {
        return !activeResponderUserIds.isEmpty();
    }

    public void onControllerStateChanged(String controllerId, String state) {
        if (controllerId == null || controllerId.isBlank() || state == null || state.isBlank()) {
            return;
        }

        String normalizedControllerId = controllerId.trim().toUpperCase(Locale.ROOT);
        String normalizedState = state.trim().toUpperCase(Locale.ROOT);
        Long userId = controllerToUserIds.get(normalizedControllerId);
        if (userId == null) {
            return;
        }

        if ("OFFLINE".equals(normalizedState)) {
            activeResponderUserIds.remove(userId);
            disconnectedUserIds.add(userId);

            String gameState = currentGame.getState();
            if ("COUNTDOWN".equalsIgnoreCase(gameState) || "PRE_QUESTION".equalsIgnoreCase(gameState)) {
                resetToLobby("Countdown abgebrochen. Ein Spieler hat sich abgemeldet.");
                return;
            }
            if ("QUESTION".equalsIgnoreCase(gameState)
                    && currentQuestionPointer >= 0
                    && currentQuestionPointer < plannedQuestions.size()) {
                tryFinishQuestionEarly(plannedQuestions.get(currentQuestionPointer));
            }
            return;
        }

        if ("CONNECTED".equals(normalizedState) || "READY".equals(normalizedState) || "NOT_READY".equals(normalizedState)) {
            if (!disconnectedUserIds.contains(userId)) {
                activeResponderUserIds.add(userId);
            }
        }
    }

    /**
     * Startet die Auswertung sobald:
     * 1. Alle Spieler (Hardware + Web) mit Controller geantwortet haben → Auswertung sofort
     * 2. Alle Spieler disconnected sind → Auswertung sofort
     * 3. Sonst → der 30-s-Timer löst die Auswertung aus
     */
    private void tryFinishQuestionEarly(JsonObject fullQuestion) {
        if (!"QUESTION".equalsIgnoreCase(currentGame.getState())) {
            return;
        }

        // Teilnehmer = Spieler mit Controller (Hardware oder Web), nicht disconnected
        Set<Long> participants = new HashSet<>();
        for (Map.Entry<Long, String> e : gamePlayerControllerIds.entrySet()) {
            if (e.getValue() != null && !e.getValue().isBlank() && !disconnectedUserIds.contains(e.getKey())) {
                participants.add(e.getKey());
            }
        }

        if (participants.isEmpty()) {
            cancelQuestionTimer();
            questionTimerId = null;
            startEvaluationPhase(fullQuestion);
            return;
        }

        // Übergang zur Auswertung wenn alle geantwortet haben ODER 30 s vergangen (Timer)
        boolean allParticipantsAnswered = true;
        for (Long userId : participants) {
            if (!currentQuestionAnswers.containsKey(userId)) {
                allParticipantsAnswered = false;
                break;
            }
        }

        if (allParticipantsAnswered) {
            cancelQuestionTimer();
            questionTimerId = null;
            startEvaluationPhase(fullQuestion);
            logger.info("Alle haben geantwortet – Übergang zur Auswertung ({} Teilnehmer)", participants.size());
        }
    }

    private void startEndPhase() {
        cancelQuestionTimer();
        cancelEvaluationTimer();

        currentGame.setState("END");
        currentGame.setCountdownSeconds(null);
        currentGame.setCurrentQuestion(null);
        publishGameStateChanged();
        currentGame.setMessage("Spiel beendet. Endergebnis wird angezeigt.");
        currentGame.setEvaluationPlayers(new JsonArray());

        JsonArray finalResults = new JsonArray();
        for (Map.Entry<Long, String> player : gamePlayerNames.entrySet()) {
            Long userId = player.getKey();
            double total = gameTotalScores.getOrDefault(userId, 0.0);
            finalResults.add(new JsonObject()
                    .put("userId", userId)
                    .put("name", player.getValue())
                    .put("totalScore", total)
                    .put("totalScoreLabel", formatScore(total)));
        }
        currentGame.setFinalResults(finalResults);

        persistHighscores(finalResults);

        // END-Status sofort an Hardware-Controller pushen für Anzeige auf OLED
        pushStatusToGameControllers();
    }

    private void persistHighscores(JsonArray finalResults) {
        String roundLength = toRoundLength(questionCount);
        for (Object item : finalResults) {
            if (!(item instanceof JsonObject result)) {
                continue;
            }
            Long userId = result.getLong("userId");
            if (userId == null) {
                continue;
            }

            double totalScore = result.getDouble("totalScore", 0.0);
            gameRepository.insertHighscore(roundLength, userId, totalScore)
                    .onFailure(error -> logger.warn("Could not persist highscore for user {}: {}", userId, error.getMessage()));
        }
    }

    private String toRoundLength(int totalQuestions) {
        if (totalQuestions >= 20) {
            return "Q20";
        }
        if (totalQuestions >= 10) {
            return "Q10";
        }
        return "Q5";
    }

    private String formatScore(double score) {
        if (Math.rint(score) == score) {
            return ((long) score) + " Punkte";
        }
        return String.format(Locale.ROOT, "%.2f Punkte", score);
    }

    private double basePointsForDifficulty(String difficulty) {
        return switch (difficulty) {
            case "HARD" -> 3.0;
            case "MEDIUM" -> 2.0;
            default -> 1.0;
        };
    }

    private double timeFactor(long elapsedMs) {
        if (elapsedMs < 5000) {
            return 1.0;
        }
        if (elapsedMs < 10000) {
            return 0.9;
        }
        if (elapsedMs < 15000) {
            return 0.8;
        }
        if (elapsedMs < 20000) {
            return 0.7;
        }
        if (elapsedMs < 25000) {
            return 0.6;
        }
        if (elapsedMs < 30000) {
            return 0.5;
        }
        return 0.0;
    }

    private JsonObject toPublicQuestion(JsonObject fullQuestion) {
        if (fullQuestion == null) {
            return null;
        }
        JsonObject copy = fullQuestion.copy();
        copy.remove("correctOption");
        return copy;
    }

    private void cancelQuestionTimer() {
        if (questionTimerId != null) {
            vertx.cancelTimer(questionTimerId);
            questionTimerId = null;
        }
    }

    private void cancelEvaluationTimer() {
        if (evaluationTimerId != null) {
            vertx.cancelTimer(evaluationTimerId);
            evaluationTimerId = null;
        }
    }
}
