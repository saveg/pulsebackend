package com.pulsebackend.clients.api;

import com.pulsebackend.base.BaseApiClient;
import com.pulsebackend.components.request.ApiControllerOptions;
import com.pulsebackend.config.ConfigLoader;
import com.pulsebackend.controllers.api.SandboxController;

public class SandboxClient extends BaseApiClient {
    public final SandboxController sandbox;

    public SandboxClient() {
        this(ApiControllerOptions.empty());
    }

    public SandboxClient(ApiControllerOptions options) {
        super(
                ApiControllerOptions.builder()
                    .baseUrl(ConfigLoader.getValue("service.url"))
                    .build()
        );
        sandbox = new SandboxController(requestOptions);
    }
}
