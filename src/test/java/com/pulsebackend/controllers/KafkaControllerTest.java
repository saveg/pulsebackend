package com.pulsebackend.controllers;

import com.pulsebackend.kafka.KafkaOperations;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class KafkaControllerTest {
    @Test
    public void listenShouldRegisterOnlyOnce() {
        FakeKafkaOperations operations = new FakeKafkaOperations();
        KafkaController<TestMessage> controller = new KafkaController<>(
                "test.topic",
                TestMessage.class,
                "test-group",
                operations
        );

        controller.listen();
        controller.listen();

        assertEquals(operations.listenCalls.get(), 1);
        assertSame(operations.storage, controller.getStorage());
        controller.close();
        assertEquals(operations.closeCalls.get(), 1);
    }

    @Test
    public void sendShouldForwardMessageToKafkaOperations() {
        FakeKafkaOperations operations = new FakeKafkaOperations();
        KafkaController<TestMessage> controller = new KafkaController<>(
                "test.topic",
                TestMessage.class,
                "test-group",
                operations
        );
        TestMessage message = new TestMessage("id-1", "created", 10);

        controller.send(message);

        assertEquals(operations.sentMessages, List.of(message));
        assertEquals(operations.sentTopics, List.of("test.topic"));
    }

    @Test
    public void findAllShouldUsePartialMatch() {
        FakeKafkaOperations operations = new FakeKafkaOperations();
        KafkaController<TestMessage> controller = new KafkaController<>(
                "test.topic",
                TestMessage.class,
                "test-group",
                operations
        );
        controller.getStorage().add(new TestMessage("id-1", "created", 10));
        controller.getStorage().add(new TestMessage("id-2", "updated", 20));

        List<TestMessage> foundMessages = controller.findAll(new TestMessage(null, "created", null));

        assertEquals(foundMessages, List.of(new TestMessage("id-1", "created", 10)));
    }

    public record TestMessage(String id, String type, Integer amount) {
    }

    private static final class FakeKafkaOperations implements KafkaOperations {
        private final AtomicInteger listenCalls = new AtomicInteger();
        private final AtomicInteger closeCalls = new AtomicInteger();
        private final List<String> sentTopics = new ArrayList<>();
        private final List<Object> sentMessages = new ArrayList<>();
        private List<?> storage;

        @Override
        public void send(String topic, String key, Object message) {
            sentTopics.add(topic);
            sentMessages.add(message);
        }

        @Override
        public <T> AutoCloseable listenToList(String topic, String groupId, Class<T> type, List<T> storage) {
            listenCalls.incrementAndGet();
            this.storage = storage;
            return closeCalls::incrementAndGet;
        }

        @Override
        public void close() {
        }
    }
}
