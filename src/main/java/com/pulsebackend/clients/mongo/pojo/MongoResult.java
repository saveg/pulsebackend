package com.pulsebackend.clients.mongo.pojo;

import com.pulsebackend.clients.api.pojo.SandboxRequestBody;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MongoResult extends SandboxRequestBody {
    private String requestId;
}