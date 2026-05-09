package com.pulsebackend.clients.kafka;

import com.pulsebackend.config.ConfigLoader;
import com.pulsebackend.controllers.KafkaController;

public final class KafkaClient {
    private static final KafkaClient INSTANCE = new KafkaClient();

    public final KafkaController<ExampleKafkaMessage> exampleEvents;
    public final KafkaController<ExampleKafkaMessage> exampleResults;

    private KafkaClient() {
        exampleEvents = new KafkaController<>(
                ConfigLoader.getValue("kafka.topics.example.events", "pulse.example.events"),
                ExampleKafkaMessage.class
        );
        exampleResults = new KafkaController<>(
                ConfigLoader.getValue("kafka.topics.example.results", "pulse.example.results"),
                ExampleKafkaMessage.class
        );
    }

    public static KafkaClient getInstance() {
        return INSTANCE;
    }

    public record ExampleKafkaMessage(String id, String type, String payload) {
    }
}
