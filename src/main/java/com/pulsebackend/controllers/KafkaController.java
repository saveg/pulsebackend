package com.pulsebackend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pulsebackend.kafka.KafkaOperations;
import com.pulsebackend.managers.KafkaManager;
import com.pulsebackend.utils.DeepPartialMatcher;
import com.pulsebackend.utils.Wait;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaController<T> implements AutoCloseable {
    private final String topic;
    private final Class<T> type;
    private final String groupId;
    private final KafkaOperations kafkaOperations;
    private final List<T> storage = new CopyOnWriteArrayList<>();
    private final AtomicBoolean listening = new AtomicBoolean(false);
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private volatile AutoCloseable registration;

    public KafkaController(String topic, Class<T> type) {
        this(topic, type, "tests." + UUID.randomUUID(), null);
    }

    public KafkaController(String topic, Class<T> type, String groupId) {
        this(topic, type, groupId, null);
    }

    public KafkaController(String topic, Class<T> type, String groupId, KafkaOperations kafkaOperations) {
        this.topic = topic;
        this.type = type;
        this.groupId = groupId;
        this.kafkaOperations = kafkaOperations;
    }

    @Step("Start Kafka listener for topic {this.topic}")
    public synchronized KafkaController<T> listen() {
        if (listening.compareAndSet(false, true)) {
            registration = kafkaOperations().listenToList(topic, groupId, type, storage);
            Allure.addAttachment("Kafka listener started", "topic=" + topic + "\ngroupId=" + groupId);
        }
        return this;
    }

    @Step("Send Kafka message to topic {this.topic}")
    public void send(T message) {
        Allure.addAttachment("Kafka message", "application/json", toJsonSafe(message), ".json");
        kafkaOperations().send(topic, null, message);
    }

    @Step("Send Kafka messages to topic {this.topic}")
    public void sendMany(List<T> messages) {
        messages.forEach(this::send);
    }

    public List<T> findAll(T partial) {
        return storage.stream()
                .filter(message -> DeepPartialMatcher.matches(message, partial))
                .toList();
    }

    @Step("Find one Kafka record in topic {this.topic}")
    public T shouldBeOneRecord(T partial) {
        List<T> all = findAll(partial);
        if (all.isEmpty()) {
            throw new AssertionError("Record was not found in topic " + topic);
        }
        if (all.size() > 1) {
            throw new AssertionError("More than one record was found in topic " + topic + ": " + all.size());
        }
        Allure.addAttachment("Found Kafka record", "application/json", toJsonSafe(all.get(0)), ".json");
        return all.get(0);
    }

    @Step("Wait for one Kafka record in topic {this.topic}")
    public T waitOneRecord(T partial, long timeout, long poll) {
        return new Wait(timeout, poll).waitingFor(() -> shouldBeOneRecord(partial));
    }

    public T waitOneRecord(T partial) {
        return waitOneRecord(partial, 4_000, 500);
    }

    @Step("Wait for Kafka records in topic {this.topic}")
    public List<T> waitForRecords(T partial, int expected, long timeout, long poll) {
        return new Wait(timeout, poll).waitingFor(() -> {
            List<T> foundRecords = findAll(partial);
            if (foundRecords.size() == expected) {
                Allure.addAttachment(
                        "Found Kafka records",
                        "application/json",
                        toJsonSafe(foundRecords),
                        ".json"
                );
                return foundRecords;
            }
            throw new IllegalStateException("Expected " + expected + " records, found " + foundRecords.size());
        });
    }

    public List<T> waitForRecords(T partial, int expected) {
        return waitForRecords(partial, expected, 10_000, 500);
    }

    @Step("Wait for no Kafka records in topic {this.topic}")
    public void waitNoneRecord(T partial, long timeout, long poll) {
        waitForRecords(partial, 0, timeout, poll);
    }

    public void waitNoneRecord(T partial) {
        waitNoneRecord(partial, 4_000, 500);
    }

    public List<T> getStorage() {
        return storage;
    }

    public String getTopic() {
        return topic;
    }

    public String getGroupId() {
        return groupId;
    }

    private String toJsonSafe(Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }

    private KafkaOperations kafkaOperations() {
        return kafkaOperations == null ? KafkaManager.getInstance() : kafkaOperations;
    }

    @Override
    public synchronized void close() {
        if (registration != null) {
            try {
                registration.close();
            } catch (Exception ignored) {
                Allure.addAttachment("Kafka listener close warning", "Listener close completed with ignored errors.");
            }
            registration = null;
        }
        listening.set(false);
    }
}
