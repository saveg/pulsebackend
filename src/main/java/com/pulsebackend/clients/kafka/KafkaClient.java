package com.pulsebackend.clients.kafka;

import com.pulsebackend.clients.api.pojo.SandboxResult;
import com.pulsebackend.config.ConfigLoader;
import com.pulsebackend.controllers.KafkaController;

public final class KafkaClient {
    private static final KafkaClient INSTANCE = new KafkaClient();

    public final KafkaController<SandboxResult> exampleEvents;

    private KafkaClient() {
        exampleEvents = new KafkaController<>(
                ConfigLoader.getValue("kafka.topics.result"),
                SandboxResult.class
        );
    }

    public static KafkaClient getInstance() {
        return INSTANCE;
    }
}
