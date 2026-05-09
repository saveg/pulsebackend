package com.pulsebackend;

import com.pulsebackend.config.ConfigLoader;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class ConfigLoaderTest {
    @BeforeMethod
    public void reloadConfig() {
        ConfigLoader.reload();
    }

    @Test
    public void localConfigShouldOverrideBaseConfig() {
        assertEquals(ConfigLoader.getValue("service.url"), "http://localhost:8080");
        assertEquals(ConfigLoader.getInt("service.timeout", null), 30);
        assertEquals(ConfigLoader.getBool("feature.enabled", false), true);
    }

    @Test
    public void baseConfigShouldRemainWhenLocalDoesNotOverrideValue() {
        assertEquals(ConfigLoader.getValue("service.name"), "pulse-backend");
        assertEquals(ConfigLoader.getList("db.list"), List.of("transactions", "events"));
        assertEquals(ConfigLoader.getValue("kafka.topics.example.results"), "pulse.example.results");
    }

    @Test
    public void missingValuesShouldReturnDefaults() {
        assertNull(ConfigLoader.getValue("missing.value"));
        assertEquals(ConfigLoader.getValue("missing.value", "fallback"), "fallback");
        assertEquals(ConfigLoader.getInt("missing.int", 10), 10);
    }

    @Test
    public void kafkaLocalConfigShouldOverrideBaseConfig() {
        assertEquals(ConfigLoader.getValue("kafka.bootstrap.servers"), "localhost:29092");
        assertEquals(ConfigLoader.getValue("kafka.topics.example.events"), "pulse.example.events.local");
        assertEquals(ConfigLoader.getInt("kafka.consumer.poll.ms", null), 500);
    }
}
