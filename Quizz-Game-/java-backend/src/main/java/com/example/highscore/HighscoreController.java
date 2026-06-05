package com.example.highscore;

import com.example.http.HttpController;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HighscoreController implements HttpController {
    private final HighscoreService highscoreService;

    public HighscoreController() {
        this.highscoreService = new HighscoreService();
    }

    @Override
    public void registerRoutes(Router router) {
        router.get("/api/highscores/:questionCount").handler(this::handleGetHighscores);
    }

    private void handleGetHighscores(RoutingContext context) {
        String questionCount = context.pathParam("questionCount");
        String limitParam = context.request().getParam("limit");
        int limit = 20;
        if (limitParam != null && !limitParam.isBlank()) {
            try {
                limit = Integer.parseInt(limitParam.trim());
            } catch (NumberFormatException ignored) {
                /* use default 20 */
            }
        }

        highscoreService.getHighscoresByQuestionCount(questionCount, limit)
                .onSuccess(result -> context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(result.encode()))
                .onFailure(error -> context.response()
                        .setStatusCode(400)
                        .end(error.getMessage() == null ? "Highscores konnten nicht geladen werden." : error.getMessage()));
    }
}
