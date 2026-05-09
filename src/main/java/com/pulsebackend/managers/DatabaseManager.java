package com.pulsebackend.managers;

import com.pulsebackend.config.ConfigLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.Converter;
import org.jooq.ConverterProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DatabaseManager {
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    private final Map<String, DSLContext> contexts = new ConcurrentHashMap<>();

    private DatabaseManager() {
        if (resolveDbKeys().isEmpty()) {
            throw new IllegalStateException("db.list is empty");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeAll, "database-manager-shutdown"));
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    public List<String> configuredDatabaseKeys() {
        return resolveDbKeys();
    }

    public DSLContext dsl(String key) {
        return contexts.computeIfAbsent(key, this::createContext);
    }

    public void closeAll() {
        contexts.clear();
        dataSources.values().forEach(dataSource -> {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                try {
                    hikariDataSource.close();
                } catch (Exception ignored) {
                    // Hikari can already be closed during test cleanup or JVM shutdown.
                }
            }
        });
        dataSources.clear();
    }

    private List<String> resolveDbKeys() {
        List<String> keys = ConfigLoader.getList("db.list");
        if (keys.isEmpty()) {
            throw new IllegalStateException("db.list is not configured");
        }
        return normalize(keys);
    }

    private DSLContext createContext(String key) {
        DataSource dataSource = dataSources.computeIfAbsent(key, this::createDataSource);
        String dialectValue = configValue("db." + key + ".dialect", "POSTGRES");
        SQLDialect dialect = SQLDialect.valueOf(dialectValue.trim().toUpperCase());

        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(dataSource);
        configuration.set(dialect);
        configuration.set(converterProvider());

        return DSL.using(configuration);
    }

    private DataSource createDataSource(String key) {
        String prefix = "db." + key + ".";
        String url = Objects.requireNonNull(
                ConfigLoader.getValue(prefix + "url"),
                () -> "Missing config key: " + prefix + "url"
        );

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(configValue(prefix + "username", ""));
        hikariConfig.setPassword(configValue(prefix + "password", ""));
        hikariConfig.setMaximumPoolSize(ConfigLoader.getInt(prefix + "pool.maxSize", 10));
        hikariConfig.setMinimumIdle(ConfigLoader.getInt(prefix + "pool.minIdle", 0));
        hikariConfig.setPoolName("db-" + key);

        String driverClassName = ConfigLoader.getValue(prefix + "driverClassName");
        if (driverClassName != null && !driverClassName.isBlank()) {
            hikariConfig.setDriverClassName(driverClassName);
        }

        return new HikariDataSource(hikariConfig);
    }

    private static List<String> normalize(Collection<String> keys) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String key : keys) {
            if (key != null) {
                String value = key.trim();
                if (!value.isEmpty()) {
                    normalized.add(value);
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String configValue(String key, String defaultValue) {
        String value = ConfigLoader.getValue(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static ConverterProvider converterProvider() {
        return new ConverterProvider() {
            private final Converter<BigDecimal, String> bigDecimalToString = new Converter<>() {
                @Override
                public String from(BigDecimal databaseObject) {
                    if (databaseObject == null) {
                        return null;
                    }
                    return databaseObject.stripTrailingZeros().toPlainString();
                }

                @Override
                public BigDecimal to(String userObject) {
                    if (userObject == null || userObject.isBlank()) {
                        return null;
                    }
                    return new BigDecimal(userObject);
                }

                @Override
                public Class<BigDecimal> fromType() {
                    return BigDecimal.class;
                }

                @Override
                public Class<String> toType() {
                    return String.class;
                }
            };

            private final Converter<String, String> stringNormalizer = new Converter<>() {
                @Override
                public String from(String databaseObject) {
                    return normalizeNumericString(databaseObject);
                }

                @Override
                public String to(String userObject) {
                    return normalizeNumericString(userObject);
                }

                @Override
                public Class<String> fromType() {
                    return String.class;
                }

                @Override
                public Class<String> toType() {
                    return String.class;
                }
            };

            @SuppressWarnings("unchecked")
            @Override
            public <T, U> Converter<T, U> provide(Class<T> databaseType, Class<U> userType) {
                if (databaseType == String.class && userType == String.class) {
                    return (Converter<T, U>) stringNormalizer;
                }
                if (databaseType == BigDecimal.class && userType == String.class) {
                    return (Converter<T, U>) bigDecimalToString;
                }
                return null;
            }
        };
    }

    private static String normalizeNumericString(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            BigDecimal number = new BigDecimal(value.replace(',', '.').replace(" ", ""));
            return number.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            return value;
        }
    }
}
