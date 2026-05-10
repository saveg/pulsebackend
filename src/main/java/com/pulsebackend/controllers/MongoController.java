package com.pulsebackend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.pulsebackend.managers.MongoManager;
import com.pulsebackend.utils.Wait;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lombok.Getter;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class MongoController<T> {
    @Getter
    private final String collectionName;
    @Getter
    private final Class<T> type;
    private final MongoCollection<T> collection;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public MongoController(String collectionName, Class<T> type) {
        this(collectionName, type, MongoManager.getInstance().getCollection(collectionName, type));
    }

    MongoController(String collectionName, Class<T> type, MongoCollection<T> collection) {
        this.collectionName = collectionName;
        this.type = type;
        this.collection = collection;
    }

    @Step("Update one Mongo document")
    public UpdateResult updateOne(Map<String, ?> where, Map<String, ?> setValues) {
        if (setValues == null || setValues.isEmpty()) {
            throw new IllegalArgumentException("setValues must not be empty");
        }
        Bson filter = toFilter(where);
        Bson update = buildSetUpdate(setValues);
        UpdateResult result = collection.updateOne(filter, update);
        Allure.addAttachment(
                "Update one result",
                "Matched: " + result.getMatchedCount() + ", Modified: " + result.getModifiedCount()
        );
        return result;
    }

    @Step("Delete Mongo documents")
    public DeleteResult deleteMany(Map<String, ?> where) {
        return Allure.step("Delete documents from collection " + collectionName, () -> {
            DeleteResult result = collection.deleteMany(toFilter(where));
            Allure.addAttachment("Delete result", "Deleted count: " + result.getDeletedCount());
            return result;
        });
    }

    public T waitForOneRecord(Map<String, ?> where) {
        return waitForOneRecord(where, Duration.ofSeconds(4), Duration.ofMillis(500));
    }

    public T waitForOneRecord(Map<String, ?> where, Duration timeout, Duration pollInterval) {
        return Allure.step("Wait for one document in collection " + collectionName, () -> {
            Bson filter = toFilter(where);
            Wait wait = new Wait(timeout.toMillis(), pollInterval.toMillis());

            T record = wait.waitingFor(() -> {
                List<T> records = findAll(filter);
                if (records.size() == 1) {
                    return records.get(0);
                }
                if (records.size() > 1) {
                    throw new AssertionError("Expected exactly one document, found " + records.size());
                }
                return null;
            });

            Allure.addAttachment("Record found", "application/json", toJsonSafe(record), ".json");
            return record;
        });
    }

    public List<T> findAll(Map<String, ?> where) {
        List<T> records = findAll(toFilter(where));
        Allure.addAttachment(
                "Mongo records",
                "application/json",
                records.stream().map(this::toJsonSafe).collect(Collectors.joining("\n")),
                ".json"
        );
        return records;
    }

    Bson toFilter(Map<String, ?> where) {
        if (where == null || where.isEmpty()) {
            return new Document();
        }
        List<Bson> parts = new ArrayList<>();
        where.forEach((key, value) -> parts.add(eq(key, value)));
        return parts.size() == 1 ? parts.get(0) : Filters.and(parts);
    }

    Bson buildSetUpdate(Map<String, ?> setValues) {
        if (setValues == null || setValues.isEmpty()) {
            throw new IllegalArgumentException("setValues must not be empty");
        }
        return Updates.combine(
                setValues.entrySet().stream()
                        .map(entry -> Updates.set(entry.getKey(), entry.getValue()))
                        .toList()
        );
    }

    BsonDocument render(Bson bson) {
        return bson.toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry());
    }

    private List<T> findAll(Bson filter) {
        return collection.find(filter).into(new ArrayList<>());
    }

    private String toJsonSafe(Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }
}
