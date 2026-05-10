package com.pulsebackend.controllers.api;

import com.pulsebackend.components.request.ApiControllerOptions;
import com.pulsebackend.components.request.ApiResponse;
import com.pulsebackend.components.request.BaseApiController;
import io.qameta.allure.Step;

public class TestUtilsController extends BaseApiController {
    public TestUtilsController(ApiControllerOptions requestOptions) {
        super(requestOptions);
    }

    @Step("Trigger archiving")
    public ApiResponse<String> triggerArchiving() {
        return request().post("/qa/test-utils/trigger-archiving");
    }
}
