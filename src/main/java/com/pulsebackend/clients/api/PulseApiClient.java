package com.pulsebackend.clients.api;

import com.pulsebackend.base.BaseApiClient;
import com.pulsebackend.components.request.ApiControllerOptions;
import com.pulsebackend.config.ConfigLoader;
import com.pulsebackend.controllers.api.HealthController;
import com.pulsebackend.controllers.api.TestUtilsController;

public class PulseApiClient extends BaseApiClient {
    public final HealthController health;
    public final TestUtilsController testUtils;

    public PulseApiClient() {
        this(ApiControllerOptions.empty());
    }

    public PulseApiClient(ApiControllerOptions options) {
        super(withServiceDefaults(options));
        health = new HealthController(requestOptions);
        testUtils = new TestUtilsController(requestOptions);
    }

    private static ApiControllerOptions withServiceDefaults(ApiControllerOptions options) {
        ApiControllerOptions defaults = ApiControllerOptions.builder()
                .baseUrl(ConfigLoader.getValue("service.url", "http://localhost:8080"))
                .build();
        return defaults.merge(options);
    }
}
