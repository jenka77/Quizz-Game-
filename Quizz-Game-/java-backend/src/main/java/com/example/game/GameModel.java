package com.example.game;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GameModel {

    private String state = "LOBBY";
    private String message = "Warte auf Spielstart.";
    private Integer countdownSeconds;
    private String preQuestionPingId;
    private Integer currentQuestionIndex = 0;
    private Integer totalQuestions = 0;
    private JsonObject currentQuestion;
    private JsonArray evaluationPlayers = new JsonArray();
    private JsonArray finalResults = new JsonArray();
    private boolean gameAborted = false;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(Integer countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }

    public Integer getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(Integer currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public JsonObject getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(JsonObject currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public JsonArray getEvaluationPlayers() {
        return evaluationPlayers;
    }

    public void setEvaluationPlayers(JsonArray evaluationPlayers) {
        this.evaluationPlayers = evaluationPlayers == null ? new JsonArray() : evaluationPlayers;
    }

    public JsonArray getFinalResults() {
        return finalResults;
    }

    public void setFinalResults(JsonArray finalResults) {
        this.finalResults = finalResults == null ? new JsonArray() : finalResults;
    }

    public String getPreQuestionPingId() {
        return preQuestionPingId;
    }

    public void setPreQuestionPingId(String preQuestionPingId) {
        this.preQuestionPingId = preQuestionPingId;
    }

    public boolean isGameAborted() {
        return gameAborted;
    }

    public void setGameAborted(boolean gameAborted) {
        this.gameAborted = gameAborted;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("state", state)
                .put("message", message)
                .put("countdownSeconds", countdownSeconds)
                .put("currentQuestionIndex", currentQuestionIndex)
                .put("totalQuestions", totalQuestions)
                .put("currentQuestion", currentQuestion)
                .put("evaluationPlayers", evaluationPlayers)
                .put("finalResults", finalResults);
        if (preQuestionPingId != null) {
            json.put("preQuestionPingId", preQuestionPingId);
        }
        json.put("gameAborted", gameAborted);
        return json;
    }

}
