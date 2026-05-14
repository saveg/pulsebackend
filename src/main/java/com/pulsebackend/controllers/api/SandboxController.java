package com.pulsebackend.controllers.api;

import com.pulsebackend.clients.api.pojo.SandboxResult;
import com.pulsebackend.components.request.ApiControllerOptions;
import com.pulsebackend.components.request.ApiResponse;
import com.pulsebackend.components.request.BaseApiController;
import com.pulsebackend.clients.api.pojo.SandboxRequestBody;
import io.qameta.allure.Step;

public class SandboxController extends BaseApiController {
    public SandboxController(ApiControllerOptions requestOptions) {
        super(requestOptions);
    }

    @Step("Send sandbox request")
    public ApiResponse<SandboxResult> sendSandboxRequest(SandboxRequestBody json) {
        return request()
                .json(json)
                .post("/api/v1/sandbox", SandboxResult.class);
    }
}
