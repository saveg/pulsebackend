package com.pulsebackend.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pulsebackend.config.ConfigLoader;
import com.pulsebackend.kafka.KafkaOperations;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class KafkaManager implements KafkaOperations {
    private static final Logger LOGGER = LogManager.getLogger(KafkaManager.class);
    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static volatile KafkaManager instance;

    private final Producer<String, String> producer;
    private final Admin admin;
    private final ObjectMapper mapper;
    private final ConcurrentMap<String, ListenerRegistration> listeners = new ConcurrentHashMap<>();

    public static KafkaManager getInstance() {
        if (instance == null) {
            synchronized (KafkaManager.class) {
                if (instance == null) {
                    instance = new KafkaManager();
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            instance.close();
                        } catch (Exception ignored) {
                            LOGGER.debug("Kafka manager shutdown completed with ignored errors.");
                        }
                    }, "kafka-manager-shutdown"));
                }
            }
        }
        return instance;
    }

    private KafkaManager() {
        this(
                new KafkaProducer<>(producerProperties(bootstrapServers())),
                Admin.create(adminProperties(bootstrapServers())),
                objectMapper()
        );
    }

    KafkaManager(Producer<String, String> producer, Admin admin, ObjectMapper mapper) {
        this.producer = Objects.requireNonNull(producer, "producer must not be null");
        this.admin = admin;
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public void send(String topic, String key, Object message) {
        try {
            String value = message instanceof String text ? text : mapper.writeValueAsString(message);
            producer.send(new ProducerRecord<>(topic, key, value), (metadata, exception) -> {
                if (exception != null) {
                    LOGGER.error("Kafka send failed for topic {}", topic, exception);
                }
            }).get();
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka message send failed", exception);
        }
    }

    public void send(String topic, Object message) {
        send(topic, null, message);
    }

    public <T> AutoCloseable listen(String topic, String groupId, Class<T> type, Consumer<T> sink) {
        String listenerKey = listenerKey(topic, groupId);
        return listeners.computeIfAbsent(listenerKey, ignored -> createListener(topic, groupId, type, sink));
    }

    @Override
    public <T> AutoCloseable listenToList(String topic, String groupId, Class<T> type, List<T> storage) {
        return listen(topic, groupId, type, storage::add);
    }

    public void stop(String topic, String groupId) {
        ListenerRegistration registration = listeners.remove(listenerKey(topic, groupId));
        if (registration != null) {
            registration.close();
        }
    }

    private <T> ListenerRegistration createListener(String topic, String groupId, Class<T> type, Consumer<T> sink) {
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<KafkaConsumer<String, String>> consumerRef = new AtomicReference<>();
        CountDownLatch assigned = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties(groupId));
            consumerRef.set(consumer);
            consumer.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    // No-op. Offsets are committed after records are processed.
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    assigned.countDown();
                }
            });
            poll(topic, type, sink, consumer, running);
        },
                "kafka-listener-" + topic + "-" + groupId);
        thread.setDaemon(true);
        thread.start();

        awaitAssignment(topic, groupId, assigned);

        return new ListenerRegistration(consumerRef, thread, running);
    }

    private void awaitAssignment(String topic, String groupId, CountDownLatch assigned) {
        try {
            if (!assigned.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Kafka listener was not assigned to topic " + topic + " for group " + groupId
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting for Kafka listener assignment for topic " + topic,
                    exception
            );
        }
    }

    private <T> void poll(
            String topic,
            Class<T> type,
            Consumer<T> sink,
            KafkaConsumer<String, String> consumer,
            AtomicBoolean running
    ) {
        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = pollRecords(consumer, running);
                if (records == null || records.isEmpty()) {
                    continue;
                }

                records.forEach(record -> {
                    if (record.value() == null) {
                        return;
                    }
                    try {
                        sink.accept(deserialize(record.value(), type));
                    } catch (Exception exception) {
                        LOGGER.error("Kafka message parsing failed for topic {}", topic, exception);
                    }
                });
                commit(consumer, topic);
            }
        } finally {
            closeConsumer(consumer);
        }
    }

    private ConsumerRecords<String, String> pollRecords(KafkaConsumer<String, String> consumer, AtomicBoolean running) {
        try {
            return consumer.poll(Duration.ofMillis(consumerPollMillis()));
        } catch (WakeupException exception) {
            if (!running.get()) {
                return null;
            }
            throw exception;
        } catch (Exception exception) {
            LOGGER.error("Kafka polling failed", exception);
            return ConsumerRecords.empty();
        }
    }

    private void commit(KafkaConsumer<String, String> consumer, String topic) {
        try {
            consumer.commitSync();
        } catch (Exception exception) {
            LOGGER.error("Kafka commit failed for topic {}", topic, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(String value, Class<T> type) throws Exception {
        if (String.class.equals(type)) {
            return (T) value;
        }
        return mapper.readValue(value, type);
    }

    private void closeConsumer(KafkaConsumer<String, String> consumer) {
        try {
            consumer.close(Duration.ofSeconds(5));
        } catch (Exception exception) {
            LOGGER.debug("Kafka consumer close failed.", exception);
        }
    }

    @Override
    public void close() {
        listeners.forEach((key, registration) -> {
            try {
                registration.close();
            } catch (Exception exception) {
                LOGGER.debug("Kafka listener close failed for {}", key, exception);
            }
        });
        listeners.clear();
        try {
            producer.flush();
        } catch (Exception exception) {
            LOGGER.debug("Kafka producer flush failed.", exception);
        }
        try {
            producer.close(Duration.ofSeconds(5));
        } catch (Exception exception) {
            LOGGER.debug("Kafka producer close failed.", exception);
        }
        if (admin != null) {
            try {
                admin.close(Duration.ofSeconds(5));
            } catch (Exception exception) {
                LOGGER.debug("Kafka admin close failed.", exception);
            }
        }
    }

    private static String listenerKey(String topic, String groupId) {
        return topic + "|" + groupId;
    }

    private static String bootstrapServers() {
        return ConfigLoader.getValue("kafka.bootstrap.servers", DEFAULT_BOOTSTRAP_SERVERS);
    }

    private static long consumerPollMillis() {
        return ConfigLoader.getInt("kafka.consumer.poll.ms", 500);
    }

    static Properties producerProperties(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.RETRIES_CONFIG, 10);
        properties.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 200);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 32_768);
        return properties;
    }

    private static Properties adminProperties(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return properties;
    }

    private static Properties consumerProperties(String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                ConfigLoader.getValue("kafka.consumer.auto.offset.reset", "latest"));
        return properties;
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static final class ListenerRegistration implements AutoCloseable {
        private final AtomicReference<KafkaConsumer<String, String>> consumerRef;
        private final Thread thread;
        private final AtomicBoolean running;

        private ListenerRegistration(
                AtomicReference<KafkaConsumer<String, String>> consumerRef,
                Thread thread,
                AtomicBoolean running
        ) {
            this.consumerRef = consumerRef;
            this.thread = thread;
            this.running = running;
        }

        @Override
        public void close() {
            if (running.compareAndSet(true, false)) {
                try {
                    KafkaConsumer<String, String> consumer = consumerRef.get();
                    if (consumer != null) {
                        consumer.wakeup();
                    }
                } catch (Exception exception) {
                    LOGGER.debug("Kafka consumer wakeup failed.", exception);
                }
                try {
                    thread.join(5_000);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
