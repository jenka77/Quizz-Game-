package com.example.game;

import java.util.ArrayList;
import java.util.List;

import com.example.database.DatabaseClient;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class GameRepository {

    private final JDBCPool jdbcPool;

    public GameRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    public Future<Integer> countMatchingQuestions(List<Long> categoryIds, List<String> difficulties) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS total
                FROM questions
                WHERE is_active = 1
                """);

        Tuple params = Tuple.tuple();

        sql.append(" AND category_id IN (");
        appendPlaceholders(sql, categoryIds.size());
        sql.append(")");
        categoryIds.forEach(params::addValue);

        sql.append(" AND difficulty IN (");
        appendPlaceholders(sql, difficulties.size());
        sql.append(")");
        difficulties.forEach(params::addValue);

        return jdbcPool.preparedQuery(sql.toString())
                .execute(params)
                .map(rows -> {
                    Row row = rows.iterator().next();
                    Object totalValue = row.getValue("total");
                    Number total = totalValue instanceof Number ? (Number) totalValue : 0;
                    return total.intValue();
                });
    }

    public Future<JsonArray> fetchCategoryDifficultyCounts() {
        return jdbcPool.preparedQuery("""
                SELECT
                    c.id AS category_id,
                    c.name AS category_name,
                    COALESCE(SUM(CASE WHEN q.is_active = 1 AND q.difficulty = 'EASY' THEN 1 ELSE 0 END), 0) AS easy_count,
                    COALESCE(SUM(CASE WHEN q.is_active = 1 AND q.difficulty = 'MEDIUM' THEN 1 ELSE 0 END), 0) AS medium_count,
                    COALESCE(SUM(CASE WHEN q.is_active = 1 AND q.difficulty = 'HARD' THEN 1 ELSE 0 END), 0) AS hard_count
                FROM categories c
                LEFT JOIN questions q ON q.category_id = c.id
                GROUP BY c.id, c.name
                ORDER BY c.id
                """)
                .execute()
                .map(rows -> {
                    JsonArray stats = new JsonArray();
                    for (Row row : rows) {
                        Number easy = (Number) row.getValue("easy_count");
                        Number medium = (Number) row.getValue("medium_count");
                        Number hard = (Number) row.getValue("hard_count");

                        stats.add(new JsonObject()
                                .put("categoryId", row.getLong("category_id"))
                                .put("name", row.getString("category_name"))
                                .put("easyCount", easy == null ? 0 : easy.intValue())
                                .put("mediumCount", medium == null ? 0 : medium.intValue())
                                .put("hardCount", hard == null ? 0 : hard.intValue()));
                    }
                    return stats;
                });
    }

    public Future<JsonObject> fetchRandomQuestion(List<Long> categoryIds, List<String> difficulties) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, category_id, difficulty, question_text, correct_option
                FROM questions
                WHERE is_active = 1
                """);

        Tuple params = Tuple.tuple();

        sql.append(" AND category_id IN (");
        appendPlaceholders(sql, categoryIds.size());
        sql.append(")");
        categoryIds.forEach(params::addValue);

        sql.append(" AND difficulty IN (");
        appendPlaceholders(sql, difficulties.size());
        sql.append(")");
        difficulties.forEach(params::addValue);

        sql.append(" ORDER BY RAND() LIMIT 1");

        return jdbcPool.preparedQuery(sql.toString())
                .execute(params)
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture("Keine passende Frage gefunden.");
                    }

                    Row row = rows.iterator().next();
                    Long questionId = row.getLong("id");
                    JsonObject question = new JsonObject()
                            .put("id", questionId)
                            .put("categoryId", row.getLong("category_id"))
                            .put("difficulty", row.getString("difficulty"))
                            .put("questionText", row.getString("question_text"))
                            .put("correctOption", row.getString("correct_option"));

                    return fetchQuestionOptions(questionId).map(options -> question.put("options", options));
                });
    }

    public Future<List<JsonObject>> fetchRandomQuestions(List<Long> categoryIds, List<String> difficulties, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, category_id, difficulty, question_text, correct_option
                FROM questions
                WHERE is_active = 1
                """);

        Tuple params = Tuple.tuple();

        sql.append(" AND category_id IN (");
        appendPlaceholders(sql, categoryIds.size());
        sql.append(")");
        categoryIds.forEach(params::addValue);

        sql.append(" AND difficulty IN (");
        appendPlaceholders(sql, difficulties.size());
        sql.append(")");
        difficulties.forEach(params::addValue);

        sql.append(" ORDER BY RAND() LIMIT ?");
        params.addValue(limit);

        return jdbcPool.preparedQuery(sql.toString())
                .execute(params)
                .compose(rows -> {
                    List<JsonObject> questions = new ArrayList<>();
                    List<Future> optionFutures = new ArrayList<>();

                    for (Row row : rows) {
                        Long questionId = row.getLong("id");
                        JsonObject question = new JsonObject()
                                .put("id", questionId)
                                .put("categoryId", row.getLong("category_id"))
                                .put("difficulty", row.getString("difficulty"))
                                .put("questionText", row.getString("question_text"))
                                .put("correctOption", row.getString("correct_option"));
                        questions.add(question);

                        optionFutures.add(fetchQuestionOptions(questionId).map(options -> {
                            question.put("options", options);
                            return null;
                        }));
                    }

                    if (questions.isEmpty()) {
                        return Future.failedFuture("Keine passenden Fragen gefunden.");
                    }

                    return CompositeFuture.all(optionFutures).map(questions);
                });
    }

    public Future<Void> insertHighscore(String roundLength, Long userId, double totalPoints) {
        return jdbcPool.preparedQuery("""
                INSERT INTO highscores (round_length, user_id, total_points, total_response_time_ms, game_session_id)
                VALUES (?, ?, ?, NULL, NULL)
                """)
                .execute(Tuple.of(roundLength, userId, totalPoints))
                .mapEmpty();
    }

    private Future<JsonArray> fetchQuestionOptions(Long questionId) {
        return jdbcPool.preparedQuery("""
                SELECT option_letter, option_text
                FROM question_options
                WHERE question_id = ?
                ORDER BY option_letter
                """)
                .execute(Tuple.of(questionId))
                .map(rows -> {
                    JsonArray options = new JsonArray();
                    for (Row row : rows) {
                        options.add(new JsonObject()
                                .put("letter", row.getString("option_letter"))
                                .put("text", row.getString("option_text")));
                    }
                    return options;
                });
    }

    private void appendPlaceholders(StringBuilder sql, int count) {
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
    }
}
