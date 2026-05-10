package com.pulsebackend.managers;

import com.pulsebackend.clients.mongo.pojo.RewardsProgram;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class MongoManagerTest {
    @Test
    public void mongoManagerShouldUseDatabaseNameFromConfigUri() {
        assertEquals(MongoManager.getInstance().getDatabase().getName(), "pulse_backend_local");
    }

    @Test
    public void mongoManagerShouldReturnTypedCollections() {
        assertEquals(
                MongoManager.getInstance().getCollection("reward_programs_local", RewardsProgram.class).getDocumentClass(),
                RewardsProgram.class
        );
    }
}
