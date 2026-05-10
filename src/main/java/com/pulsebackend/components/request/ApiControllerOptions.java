package com.pulsebackend.components.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiControllerOptions {
    private final String baseUrl;
    private final String bearerToken;
    private final BasicAuthCredentials basicAuth;
    private final Map<String, String> headers;
    private final Map<String, String> cookies;

    private ApiControllerOptions(Builder builder) {
        this.baseUrl = blankToNull(builder.baseUrl);
        this.bearerToken = blankToNull(builder.bearerToken);
        this.basicAuth = builder.basicAuth;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
        this.cookies = Collections.unmodifiableMap(new LinkedHashMap<>(builder.cookies));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ApiControllerOptions empty() {
        return builder().build();
    }

    public ApiControllerOptions merge(ApiControllerOptions overrides) {
        Builder builder = builder()
                .baseUrl(baseUrl)
                .bearerToken(bearerToken)
                .basicAuth(basicAuth)
                .headers(headers)
                .cookies(cookies);

        if (overrides == null) {
            return builder.build();
        }

        if (overrides.baseUrl != null) {
            builder.baseUrl(overrides.baseUrl);
        }
        if (overrides.bearerToken != null) {
            builder.bearerToken(overrides.bearerToken);
        }
        if (overrides.basicAuth != null) {
            builder.basicAuth(overrides.basicAuth);
        }
        builder.headers(overrides.headers);
        builder.cookies(overrides.cookies);
        return builder.build();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public BasicAuthCredentials getBasicAuth() {
        return basicAuth;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static final class Builder {
        private String baseUrl;
        private String bearerToken;
        private BasicAuthCredentials basicAuth;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final Map<String, String> cookies = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder bearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
            return this;
        }

        public Builder basicAuth(String username, String password) {
            this.basicAuth = new BasicAuthCredentials(username, password);
            return this;
        }

        public Builder basicAuth(BasicAuthCredentials basicAuth) {
            this.basicAuth = basicAuth;
            return this;
        }

        public Builder header(String name, String value) {
            if (name != null && value != null) {
                headers.put(name, value);
            }
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                headers.forEach(this::header);
            }
            return this;
        }

        public Builder cookie(String name, String value) {
            if (name != null && value != null) {
                cookies.put(name, value);
            }
            return this;
        }

        public Builder cookies(Map<String, String> cookies) {
            if (cookies != null) {
                cookies.forEach(this::cookie);
            }
            return this;
        }

        public ApiControllerOptions build() {
            return new ApiControllerOptions(this);
        }
    }
}
