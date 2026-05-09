package com.pulsebackend.kafka;

import java.util.List;

public interface KafkaOperations extends AutoCloseable {
    void send(String topic, String key, Object message);

    <T> AutoCloseable listenToList(String topic, String groupId, Class<T> type, List<T> storage);
}
