package com.pulsebackend.components.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiResponse<T> {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final int statusCode;
    private final T body;
    private final String rawBody;
    private final Map<String, String> headers;
    private final Response rawResponse;

    private ApiResponse(int statusCode, T body, String rawBody, Map<String, String> headers, Response rawResponse) {
        this.statusCode = statusCode;
        this.body = body;
        this.rawBody = rawBody;
        this.headers = headers;
        this.rawResponse = rawResponse;
    }

    public static <T> ApiResponse<T> from(Response response, Class<T> responseType) {
        String rawBody = response.asString();
        return new ApiResponse<>(
                response.statusCode(),
                bodyAs(response, rawBody, responseType),
                rawBody,
                headers(response),
                response
        );
    }

    public static <T> ApiResponse<T> from(Response response, TypeRef<T> responseType) {
        String rawBody = response.asString();
        T body = rawBody == null || rawBody.isBlank() ? null : response.as(responseType);
        return new ApiResponse<>(response.statusCode(), body, rawBody, headers(response), response);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public T getBody() {
        return body;
    }

    public String getRawBody() {
        return rawBody;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Response getRawResponse() {
        return rawResponse;
    }

    public <R> R bodyAs(Class<R> responseType) {
        return bodyAs(rawResponse, rawBody, responseType);
    }

    private static Map<String, String> headers(Response response) {
        Map<String, String> result = new LinkedHashMap<>();
        response.getHeaders().forEach(header -> result.put(header.getName(), header.getValue()));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <R> R bodyAs(Response response, String rawBody, Class<R> responseType) {
        if (responseType == null || responseType == Void.class) {
            return null;
        }
        if (responseType == String.class) {
            return (R) rawBody;
        }
        if (responseType == byte[].class) {
            return (R) response.asByteArray();
        }
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(rawBody, responseType);
        } catch (JsonProcessingException exception) {
            return response.as(responseType);
        }
    }
}
