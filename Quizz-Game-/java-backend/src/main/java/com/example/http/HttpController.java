package com.example.http;

import io.vertx.ext.web.Router;

public interface HttpController {
    public void registerRoutes(Router router);
}
