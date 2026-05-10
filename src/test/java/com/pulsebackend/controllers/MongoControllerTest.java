package com.pulsebackend.controllers;

import com.pulsebackend.clients.mongo.pojo.RewardsProgram;
import org.bson.BsonDocument;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class MongoControllerTest {
    private final MongoController<RewardsProgram> controller = new MongoController<>(
            "reward_programs",
            RewardsProgram.class
    );

    @Test
    public void toFilterShouldCreateEmptyFilterForEmptyConditions() {
        BsonDocument filter = controller.render(controller.toFilter(Map.of()));

        assertTrue(filter.isEmpty());
    }

    @Test
    public void toFilterShouldCreateEqualityFilter() {
        BsonDocument filter = controller.render(controller.toFilter(Map.of("rewardProgramExtId", "program-1")));

        assertEquals(filter, BsonDocument.parse("{\"rewardProgramExtId\":\"program-1\"}"));
    }

    @Test
    public void toFilterShouldCombineMultipleConditions() {
        BsonDocument filter = controller.render(controller.toFilter(Map.of(
                "rewardProgramExtId", "program-1",
                "createdAt", "2026-05-10"
        )));

        assertTrue(filter.containsKey("$and"));
    }

    @Test
    public void buildSetUpdateShouldCreateSetUpdate() {
        BsonDocument update = controller.render(controller.buildSetUpdate(Map.of("updatedAt", "2026-05-10")));

        assertEquals(update, BsonDocument.parse("{\"$set\":{\"updatedAt\":\"2026-05-10\"}}"));
    }

    @Test
    public void buildSetUpdateShouldRejectEmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> controller.buildSetUpdate(Map.of()));
    }
}
