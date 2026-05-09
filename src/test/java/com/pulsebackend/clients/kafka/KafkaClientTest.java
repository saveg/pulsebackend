package com.pulsebackend.clients.kafka;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class KafkaClientTest {
    @Test
    public void kafkaClientShouldExposeExampleControllersFromConfig() {
        KafkaClient client = KafkaClient.getInstance();

        assertEquals(client.exampleEvents.getTopic(), "pulse.example.events.local");
        assertEquals(client.exampleResults.getTopic(), "pulse.example.results");
    }
}
