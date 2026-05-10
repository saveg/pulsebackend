package com.pulsebackend.clients.mongo;

import com.pulsebackend.clients.mongo.pojo.RewardsProgram;
import com.pulsebackend.clients.mongo.pojo.RuleParameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class MongoClientTest {
    @Test
    public void mongoClientShouldExposeTypedControllersFromConfig() {
        MongoClient client = new MongoClient();

        assertEquals(client.rewardPrograms.getCollectionName(), "reward_programs_local");
        assertEquals(client.rewardPrograms.getType(), RewardsProgram.class);
        assertEquals(client.ruleParameters.getCollectionName(), "rule_parameters_local");
        assertEquals(client.ruleParameters.getType(), RuleParameters.class);
    }
}
