package com.pulsebackend.clients.api.pojo;

import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxResult extends SandboxRequestBody {
    private String id;

    public static SandboxResult byId(String id) {
        return SandboxResult.builder()
                .id(id)
                .customerId(null)
                .symbol(null)
                .side(null)
                .quantity(null)
                .price(null)
                .requestedAt(null)
                .build();
    }
}
