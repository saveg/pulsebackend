package com.pulsebackend.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.assertEquals;

public class KafkaManagerTest {
    @Test
    public void sendShouldSerializeObjectMessages() throws Exception {
        MockProducer<String, String> producer = new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
        KafkaManager manager = new KafkaManager(producer, null, new ObjectMapper());

        manager.send("test.topic", "message-key", new TestPayload("id-1", 42));

        ProducerRecord<String, String> record = producer.history().get(0);
        Map<?, ?> payload = new ObjectMapper().readValue(record.value(), Map.class);
        assertEquals(record.topic(), "test.topic");
        assertEquals(record.key(), "message-key");
        assertEquals(payload, Map.of("id", "id-1", "amount", 42));
    }

    @Test
    public void sendShouldKeepStringMessagesRaw() {
        MockProducer<String, String> producer = new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
        KafkaManager manager = new KafkaManager(producer, null, new ObjectMapper());

        manager.send("test.topic", "message-key", "raw-message");

        ProducerRecord<String, String> record = producer.history().get(0);
        assertEquals(record.value(), "raw-message");
    }

    @Test
    public void producerPropertiesShouldUseConfiguredBootstrapServers() {
        Properties properties = KafkaManager.producerProperties("localhost:29092");

        assertEquals(properties.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG), "localhost:29092");
        assertEquals(properties.get(ProducerConfig.ACKS_CONFIG), "all");
        assertEquals(properties.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG), "true");
    }

    public record TestPayload(String id, Integer amount) {
    }
}
