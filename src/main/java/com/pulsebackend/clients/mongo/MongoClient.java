package com.pulsebackend.clients.mongo;

import com.pulsebackend.clients.mongo.pojo.RewardsProgram;
import com.pulsebackend.clients.mongo.pojo.RuleParameters;
import com.pulsebackend.config.ConfigLoader;
import com.pulsebackend.controllers.MongoController;

public class MongoClient {
    public final MongoController<RewardsProgram> rewardPrograms;
    public final MongoController<RuleParameters> ruleParameters;

    public MongoClient() {
        rewardPrograms = new MongoController<>(
                ConfigLoader.getValue("mongo.collections.rewardPrograms", "reward_programs"),
                RewardsProgram.class
        );
        ruleParameters = new MongoController<>(
                ConfigLoader.getValue("mongo.collections.ruleParameters", "rule_parameters"),
                RuleParameters.class
        );
    }
}
