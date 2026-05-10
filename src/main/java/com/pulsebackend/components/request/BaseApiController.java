package com.pulsebackend.components.request;

public abstract class BaseApiController {
    private final ApiControllerOptions requestOptions;

    protected BaseApiController(ApiControllerOptions requestOptions) {
        this.requestOptions = requestOptions == null ? ApiControllerOptions.empty() : requestOptions;
    }

    protected LogRequest request() {
        return new LogRequest(requestOptions);
    }

    public ApiControllerOptions getRequestOptions() {
        return requestOptions;
    }
}
