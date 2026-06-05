package com.example;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.auth.AuthController;
import com.example.admin.AdminController;
import com.example.controller.ControllerController;
import com.example.database.DatabaseClient;
import com.example.game.GameController;
import com.example.game.GameStateManager;
import com.example.highscore.HighscoreController;
import com.example.http.HttpController;
import com.example.http.HttpServerVerticle;
import com.example.mqtt.MqttVerticle;
import com.example.player.PlayerController;
import com.example.player.PlayerService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        setupJDBCPool();

        ensureActiveSessionsTable()
                .onSuccess(v -> finishStartup(startPromise))
                .onFailure(err -> {
                    logger.warn("Could not ensure active_sessions table: {}. Lobby may fail.", err.getMessage());
                    finishStartup(startPromise);
                });
    }

    private void setupJDBCPool() {
        JsonObject config = new JsonObject()
                .put("DB_HOST", System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "mariadb")
                .put("DB_PORT", System.getenv("DB_PORT") != null ? Integer.parseInt(System.getenv("DB_PORT")) : 3306)
                .put("DB_NAME", System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "game")
                .put("DB_USER", System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "user")
                .put("DB_PASSWORD", System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "userpassword");

        DatabaseClient.initialize(vertx, config);
    }

    private void finishStartup(Promise<Void> startPromise) {
        GameStateManager.getInstance(vertx);
        PlayerService.startHeartbeatTimer(vertx);
        vertx.eventBus().consumer("controller.disconnected.pre-question", msg -> {
            String controllerId = msg.body() != null ? msg.body().toString() : null;
            if (controllerId != null && !controllerId.isBlank()) {
                new PlayerService().markControllerOfflineFromPreQuestion(controllerId);
            }
        });
        MqttVerticle mqtt = new MqttVerticle();
        HttpServerVerticle http = new HttpServerVerticle();
        vertx.deployVerticle(mqtt);
        vertx.deployVerticle(http);
        setupHttpVerticle(http);
        startPromise.complete();
    }

    private Future<Void> ensureActiveSessionsTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS active_sessions (
                  user_id BIGINT UNSIGNED NOT NULL,
                  auth_token VARCHAR(96) NOT NULL,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (auth_token),
                  KEY idx_active_sessions_user (user_id),
                  CONSTRAINT fk_active_sessions_user
                    FOREIGN KEY (user_id) REFERENCES users(id)
                    ON DELETE CASCADE ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """;
        return DatabaseClient.getInstance().query(sql).execute().mapEmpty();
    }

    private void setupHttpVerticle(HttpServerVerticle httpVerticle) {

        final List<HttpController> controllers = List.of(
                new AuthController(),
                new AdminController(),
                new GameController(),
                new PlayerController(),
                new ControllerController(),
                new HighscoreController()
        );

        controllers.forEach(it -> it.registerRoutes(httpVerticle.router));
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle(), res -> {
            if (res.failed()) {
                logger.error("Verticle deployment failed: {}", res.cause().getMessage());
            }
        });
    }
}
