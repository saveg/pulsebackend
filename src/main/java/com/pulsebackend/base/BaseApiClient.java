package com.pulsebackend.base;

import com.pulsebackend.components.request.ApiControllerOptions;

public abstract class BaseApiClient {
    protected final ApiControllerOptions requestOptions;

    protected BaseApiClient(ApiControllerOptions requestOptions) {
        this.requestOptions = requestOptions == null ? ApiControllerOptions.empty() : requestOptions;
    }

    public ApiControllerOptions getRequestOptions() {
        return requestOptions;
    }
}
