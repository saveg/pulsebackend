package com.pulsebackend.clients.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxRequestBody {

    @JsonProperty("customerId")
    private String customerId = "customer-1";

    @Builder.Default
    @JsonProperty("symbol")
    private String symbol = "AAPL";

    @Builder.Default
    @JsonProperty("side")
    private String side = "buy";

    @Builder.Default
    @JsonProperty("quantity")
    private Long quantity = 1L;

    @Builder.Default
    @JsonProperty("price")
    private Double price = 100.0;

    @Builder.Default
    private Instant requestedAt = new java.util.Date().toInstant();
}