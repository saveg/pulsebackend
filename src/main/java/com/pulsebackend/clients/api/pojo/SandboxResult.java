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
}