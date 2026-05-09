package com.pulsebackend.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unchecked")
public final class ConfigLoader {
    private static final String BASE_FILE = "application.yaml";
    private static final String LOCAL_FILE = "application.local.yaml";

    private static final Map<String, Object> mergedConfig = new LinkedHashMap<>();
    private static final Map<String, Object> flatConfig = new LinkedHashMap<>();
    private static final AtomicBoolean loaded = new AtomicBoolean(false);

    static {
        load();
    }

    private ConfigLoader() {
    }

    public static synchronized void reload() {
        mergedConfig.clear();
        flatConfig.clear();
        loaded.set(false);
        load();
    }

    public static String getValue(String key) {
        Object value = flatConfig.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static String getValue(String key, String defaultValue) {
        String value = getValue(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static Integer getInt(String key, Integer defaultValue) {
        String value = getValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static Boolean getBool(String key, Boolean defaultValue) {
        String value = getValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.trim());
    }

    public static List<String> getList(String key) {
        Object rawValue = flatConfig.get(key);
        if (rawValue == null) {
            return List.of();
        }

        if (rawValue instanceof Collection<?> values) {
            List<String> result = new ArrayList<>();
            for (Object value : values) {
                if (value != null) {
                    String item = String.valueOf(value).trim();
                    if (!item.isEmpty()) {
                        result.add(item);
                    }
                }
            }
            return result;
        }

        String value = String.valueOf(rawValue).trim();
        if (value.isEmpty()) {
            return List.of();
        }

        String[] parts = value.split("[,\\s]+");
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }

        return new ArrayList<>(result);
    }

    public static Map<String, String> all() {
        Map<String, String> result = new LinkedHashMap<>();
        flatConfig.forEach((key, value) -> result.put(key, String.valueOf(value)));
        return Collections.unmodifiableMap(result);
    }

    private static void load() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }

        Yaml yaml = new Yaml();
        loadClasspathYaml(yaml, BASE_FILE);
        loadClasspathYaml(yaml, LOCAL_FILE);
        flatConfig.putAll(flatten(mergedConfig, ""));
    }

    private static void loadClasspathYaml(Yaml yaml, String fileName) {
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                return;
            }

            Map<String, Object> config = yaml.load(inputStream);
            deepMerge(mergedConfig, config);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load config file: " + fileName, exception);
        }
    }

    private static void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        if (source == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object existing = target.get(entry.getKey());
            Object incoming = entry.getValue();

            if (existing instanceof Map<?, ?> && incoming instanceof Map<?, ?>) {
                deepMerge((Map<String, Object>) existing, (Map<String, Object>) incoming);
            } else {
                target.put(entry.getKey(), incoming);
            }
        }
    }

    private static Map<String, Object> flatten(Map<String, Object> source, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> nestedMap) {
                result.putAll(flatten((Map<String, Object>) nestedMap, key));
            } else {
                result.put(key, value);
            }
        }

        return result;
    }
}
