package com.pulsebackend.clients.mongo.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import java.time.Instant;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleParameters {
    @BsonProperty("_id")
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    @BsonProperty("_class")
    private String className;

    @BsonProperty("rule_id")
    private String ruleId;

    @BsonProperty("max_reward_count")
    private Map<String, String> maxRewardCount;

    private Instant createdAt;
    private Instant updatedAt;
}
