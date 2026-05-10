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
        assertEquals(ConfigLoader.getList("db.list"), List.of("events"));
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

    @Test
    public void databaseLocalConfigShouldOverrideBaseConfig() {
        assertEquals(ConfigLoader.getValue("db.events.dialect"), "H2");
        assertEquals(ConfigLoader.getValue("db.events.username"), "sa");
        assertEquals(ConfigLoader.getInt("db.events.pool.maxSize", null), 2);
        assertEquals(ConfigLoader.getValue("db.events.url"), "jdbc:h2:mem:pulse_events;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
    }

    @Test
    public void mongoLocalConfigShouldOverrideBaseConfig() {
        assertEquals(ConfigLoader.getValue("mongo.uri"), "mongodb://localhost:27017/pulse_backend_local");
        assertEquals(ConfigLoader.getValue("mongo.collections.rewardPrograms"), "reward_programs_local");
        assertEquals(ConfigLoader.getValue("mongo.collections.ruleParameters"), "rule_parameters_local");
    }
}
