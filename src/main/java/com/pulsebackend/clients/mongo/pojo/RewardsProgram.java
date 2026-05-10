package com.pulsebackend.clients.mongo.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardsProgram {
    @BsonProperty("_id")
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    @BsonProperty("_class")
    private String className;

    private String createdAt;
    private String updatedAt;
    private Map<String, String> maxReward;
    private String rewardProgramExtId;
}
