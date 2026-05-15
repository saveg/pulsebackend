package com.pulsebackend.clients.mongo;

import com.pulsebackend.clients.mongo.pojo.MongoResult;
import com.pulsebackend.config.ConfigLoader;
import com.pulsebackend.controllers.MongoController;

public class MongoClient {
    public final MongoController<MongoResult> sandboxOrders;

    public MongoClient() {
        sandboxOrders = new MongoController<>(
                ConfigLoader.getValue("mongo.collections.sandboxOrders"),
                MongoResult.class
        );
    }
}
