package com.sandboxservice;

import com.pulsebackend.clients.api.SandboxClient;
import com.pulsebackend.clients.api.pojo.SandboxRequestBody;
import com.pulsebackend.clients.api.pojo.SandboxResult;
import com.pulsebackend.clients.db.SandboxDb;
import com.pulsebackend.clients.kafka.KafkaClient;
import com.pulsebackend.clients.mongo.MongoClient;
import com.pulsebackend.utils.DeepSubsetAssertions;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

public class SandboxTest {
    @Test
    public void sandboxPathTest() {
        KafkaClient.getInstance().exampleEvents.listen();
        var body = SandboxRequestBody.builder()
                .customerId("test-1")
                .build();
        var response = new SandboxClient().sandbox.sendSandboxRequest(body);
        var kafkaResult = KafkaClient.getInstance().exampleEvents.waitOneRecord(
                SandboxResult.byId(response.getBody().getId())
        );
        DeepSubsetAssertions.assertContainsSubset(kafkaResult, response.getBody(), "Response body");
        DeepSubsetAssertions.assertContainsSubset(kafkaResult, body, "Kafka message");
        var mongoRecord = new MongoClient().sandboxOrders.waitForOneRecord(
                Map.of("requestId", response.getBody().getId())
        );
        DeepSubsetAssertions.assertContainsSubset(mongoRecord, body, "Mongo record");
        UUID id = UUID.fromString(response.getBody().getId());
        var sqlRecord = new SandboxDb().sandboxOrders.waitForOneRecord(
                Map.of("id", id)
        );
        DeepSubsetAssertions.assertContainsSubset(sqlRecord, body, "Postgres record");
    }
}
