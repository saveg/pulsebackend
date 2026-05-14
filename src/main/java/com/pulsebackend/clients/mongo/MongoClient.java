package com.pulsebackend.clients.mongo;

import com.pulsebackend.clients.api.pojo.SandboxResult;
import com.pulsebackend.config.ConfigLoader;
import com.pulsebackend.controllers.MongoController;

public class MongoClient {
    public final MongoController<SandboxResult> sandboxOrders;

    public MongoClient() {
        sandboxOrders = new MongoController<>(
                ConfigLoader.getValue("mongo.collections.sandboxOrders"),
                SandboxResult.class
        );
    }
}
