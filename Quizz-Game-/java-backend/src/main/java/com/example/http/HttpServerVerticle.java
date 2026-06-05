package com.example.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class HttpServerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);

    public Router router = Router.router(vertx);

    @Override
    public void start() {
        // Configure Router
        router.route().handler(BodyHandler.create());

        // CORS Configuration
        Set<String> allowedHeaders = Set.of(
            "x-requested-with",
            "Access-Control-Allow-Origin",
            "origin",
            "Content-Type",
            "accept"
        );
        router.route().handler(CorsHandler.create().allowedHeaders(allowedHeaders));

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080, http -> {
                if (http.failed()) {
                    logger.error("Failed to start HTTP server: {}", http.cause().getMessage());
                }
            });
    }
}
