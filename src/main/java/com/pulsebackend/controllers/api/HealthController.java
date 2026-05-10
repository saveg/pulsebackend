package com.pulsebackend.controllers.api;

import com.pulsebackend.components.request.ApiControllerOptions;
import com.pulsebackend.components.request.ApiResponse;
import com.pulsebackend.components.request.BaseApiController;
import io.qameta.allure.Step;

public class HealthController extends BaseApiController {
    public HealthController(ApiControllerOptions requestOptions) {
        super(requestOptions);
    }

    @Step("Get service health")
    public ApiResponse<HealthResponse> getHealth() {
        return request().get("/actuator/health", HealthResponse.class);
    }

    public record HealthResponse(String status) {
    }
}
