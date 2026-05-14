package com.sandboxservice;

import com.pulsebackend.clients.api.SandboxClient;
import com.pulsebackend.clients.api.pojo.SandboxRequestBody;
import com.pulsebackend.clients.api.pojo.SandboxResult;
import com.pulsebackend.clients.kafka.KafkaClient;
import org.testng.annotations.Test;

public class SandboxTest {
    @Test
    public void sandboxPathTest() {
        KafkaClient.getInstance().exampleEvents.listen();
        var body = SandboxRequestBody.builder()
                .customerId("test-1")
                .symbol("AAPL")
                .side("buy")
                .quantity(1L)
                .price(100.0)
                .requestedAt("2026-05-12T00:00:00Z")
                .build();
        var response = new SandboxClient().sandbox.sendSandboxRequest(body);
        var kafkaResult = KafkaClient.getInstance().exampleEvents.waitOneRecord(
                SandboxResult.builder().id(response.getBody().getId()).build()
        );

    }
}
