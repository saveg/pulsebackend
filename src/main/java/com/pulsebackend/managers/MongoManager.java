package com.pulsebackend.managers;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClients;
import com.pulsebackend.config.ConfigLoader;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public final class MongoManager implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(MongoManager.class);
    private static final MongoManager INSTANCE = new MongoManager();

    private final com.mongodb.client.MongoClient mongoClient;
    private final MongoDatabase database;

    private MongoManager() {
        String uri = ConfigLoader.getValue("mongo.uri");
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("mongo.uri must be provided");
        }

        ConnectionString connectionString = new ConnectionString(uri);
        String databaseName = connectionString.getDatabase();
        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalStateException("mongo.uri must include a database name");
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(pojoCodecRegistry())
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(databaseName);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "mongo-manager-shutdown"));
    }

    public static MongoManager getInstance() {
        return INSTANCE;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public com.mongodb.client.MongoClient getClient() {
        return mongoClient;
    }

    public MongoCollection<Document> getCollection(String name) {
        return database.getCollection(name);
    }

    public <T> MongoCollection<T> getCollection(String name, Class<T> model) {
        return database.getCollection(name, model);
    }

    @Override
    public void close() {
        try {
            mongoClient.close();
        } catch (Exception exception) {
            LOGGER.debug("Mongo client close failed.", exception);
        }
    }

    private static CodecRegistry pojoCodecRegistry() {
        return fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder()
                        .conventions(Conventions.DEFAULT_CONVENTIONS)
                        .automatic(true)
                        .build())
        );
    }
}
