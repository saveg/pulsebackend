package com.pulsebackend.components.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Allure;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LogRequest {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final RestAssuredConfig restAssuredConfig = RestAssuredConfig.config()
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory((type, charset) -> mapper));

    private String baseUrl;
    private BasicAuthCredentials basicAuth;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private final Map<String, Object> queryParams = new LinkedHashMap<>();
    private String rawQuery;
    private Object body;
    private ContentType contentType;
    private boolean jsonBody;
    private boolean formBody;

    public LogRequest() {
        this(ApiControllerOptions.empty());
    }

    public LogRequest(ApiControllerOptions options) {
        ApiControllerOptions safeOptions = options == null ? ApiControllerOptions.empty() : options;
        baseUrl(safeOptions.getBaseUrl());
        baseAuth(safeOptions.getBasicAuth());
        headers(safeOptions.getHeaders());
        cookies(safeOptions.getCookies());
        bearerToken(safeOptions.getBearerToken());
    }

    public LogRequest baseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            this.baseUrl = removeTrailingSlash(baseUrl);
        }
        return this;
    }

    public LogRequest baseAuth(String username, String password) {
        return baseAuth(new BasicAuthCredentials(username, password));
    }

    public LogRequest baseAuth(BasicAuthCredentials credentials) {
        if (credentials != null) {
            this.basicAuth = credentials;
        }
        return this;
    }

    public LogRequest bearerToken(String bearerToken) {
        if (bearerToken != null && !bearerToken.isBlank()) {
            header("Authorization", "Bearer " + bearerToken);
        }
        return this;
    }

    public LogRequest header(String name, String value) {
        if (name != null && value != null) {
            headers.put(name, value);
        }
        return this;
    }

    public LogRequest headers(Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(this::header);
        }
        return this;
    }

    public LogRequest cookie(String name, String value) {
        if (name != null && value != null) {
            cookies.put(name, value);
        }
        return this;
    }

    public LogRequest cookies(Map<String, String> cookies) {
        if (cookies != null) {
            cookies.forEach(this::cookie);
        }
        return this;
    }

    public LogRequest queryParam(String name, Object value) {
        if (name != null && value != null) {
            queryParams.put(name, value);
        }
        return this;
    }

    public LogRequest queryParams(Map<String, ?> queryParams) {
        if (queryParams != null) {
            queryParams.forEach(this::queryParam);
        }
        return this;
    }

    public LogRequest qs(String queryString) {
        if (queryString != null && !queryString.isBlank()) {
            rawQuery = queryString.startsWith("?") ? queryString.substring(1) : queryString;
        }
        return this;
    }

    public LogRequest json(Object body) {
        this.body = body;
        this.contentType = ContentType.JSON;
        this.jsonBody = true;
        this.formBody = false;
        return this;
    }

    public LogRequest body(Object body) {
        this.body = body;
        this.formBody = false;
        return this;
    }

    public LogRequest formParams(Map<String, ?> formParams) {
        this.body = formParams == null ? Map.of() : new LinkedHashMap<>(formParams);
        this.contentType = ContentType.URLENC;
        this.formBody = true;
        this.jsonBody = false;
        return this;
    }

    public ApiResponse<String> get(String pathname) {
        return get(pathname, String.class);
    }

    public <T> ApiResponse<T> get(String pathname, Class<T> responseType) {
        return makeSuccess(pathname, Method.GET, responseType);
    }

    public <T> ApiResponse<T> get(String pathname, TypeRef<T> responseType) {
        return makeSuccess(pathname, Method.GET, responseType);
    }

    public ApiResponse<String> post(String pathname) {
        return post(pathname, String.class);
    }

    public <T> ApiResponse<T> post(String pathname, Class<T> responseType) {
        return makeSuccess(pathname, Method.POST, responseType);
    }

    public <T> ApiResponse<T> post(String pathname, TypeRef<T> responseType) {
        return makeSuccess(pathname, Method.POST, responseType);
    }

    public ApiResponse<String> put(String pathname) {
        return put(pathname, String.class);
    }

    public <T> ApiResponse<T> put(String pathname, Class<T> responseType) {
        return makeSuccess(pathname, Method.PUT, responseType);
    }

    public <T> ApiResponse<T> put(String pathname, TypeRef<T> responseType) {
        return makeSuccess(pathname, Method.PUT, responseType);
    }

    public ApiResponse<String> patch(String pathname) {
        return patch(pathname, String.class);
    }

    public <T> ApiResponse<T> patch(String pathname, Class<T> responseType) {
        return makeSuccess(pathname, Method.PATCH, responseType);
    }

    public <T> ApiResponse<T> patch(String pathname, TypeRef<T> responseType) {
        return makeSuccess(pathname, Method.PATCH, responseType);
    }

    public ApiResponse<String> delete(String pathname) {
        return delete(pathname, String.class);
    }

    public <T> ApiResponse<T> delete(String pathname, Class<T> responseType) {
        return makeSuccess(pathname, Method.DELETE, responseType);
    }

    public <T> ApiResponse<T> delete(String pathname, TypeRef<T> responseType) {
        return makeSuccess(pathname, Method.DELETE, responseType);
    }

    public ApiResponse<String> getError(String pathname) {
        return getError(pathname, String.class);
    }

    public <T> ApiResponse<T> getError(String pathname, Class<T> responseType) {
        return makeError(pathname, Method.GET, responseType);
    }

    public ApiResponse<String> postError(String pathname) {
        return postError(pathname, String.class);
    }

    public <T> ApiResponse<T> postError(String pathname, Class<T> responseType) {
        return makeError(pathname, Method.POST, responseType);
    }

    public ApiResponse<String> putError(String pathname) {
        return putError(pathname, String.class);
    }

    public <T> ApiResponse<T> putError(String pathname, Class<T> responseType) {
        return makeError(pathname, Method.PUT, responseType);
    }

    public ApiResponse<String> patchError(String pathname) {
        return patchError(pathname, String.class);
    }

    public <T> ApiResponse<T> patchError(String pathname, Class<T> responseType) {
        return makeError(pathname, Method.PATCH, responseType);
    }

    public ApiResponse<String> deleteError(String pathname) {
        return deleteError(pathname, String.class);
    }

    public <T> ApiResponse<T> deleteError(String pathname, Class<T> responseType) {
        return makeError(pathname, Method.DELETE, responseType);
    }

    private <T> ApiResponse<T> makeSuccess(String pathname, Method method, Class<T> responseType) {
        ApiResponse<T> response = makeRequest(pathname, method, responseType);
        if (response.getStatusCode() > 399) {
            throw new AssertionError("StatusCode should be 1xx, 2xx or 3xx. Actual: " + response.getStatusCode());
        }
        return response;
    }

    private <T> ApiResponse<T> makeSuccess(String pathname, Method method, TypeRef<T> responseType) {
        ApiResponse<T> response = makeRequest(pathname, method, responseType);
        if (response.getStatusCode() > 399) {
            throw new AssertionError("StatusCode should be 1xx, 2xx or 3xx. Actual: " + response.getStatusCode());
        }
        return response;
    }

    private <T> ApiResponse<T> makeError(String pathname, Method method, Class<T> responseType) {
        ApiResponse<T> response = makeRequest(pathname, method, responseType);
        if (response.getStatusCode() < 400) {
            throw new AssertionError("StatusCode should be 4xx or 5xx. Actual: " + response.getStatusCode());
        }
        return response;
    }

    private <T> ApiResponse<T> makeRequest(String pathname, Method method, Class<T> responseType) {
        return Allure.step(stepName(pathname, method), () -> {
            logRequest(pathname, method);
            Response response = send(pathname, method);
            ApiResponse<T> apiResponse = ApiResponse.from(response, responseType);
            logResponse(apiResponse);
            return apiResponse;
        });
    }

    private <T> ApiResponse<T> makeRequest(String pathname, Method method, TypeRef<T> responseType) {
        return Allure.step(stepName(pathname, method), () -> {
            logRequest(pathname, method);
            Response response = send(pathname, method);
            ApiResponse<T> apiResponse = ApiResponse.from(response, responseType);
            logResponse(apiResponse);
            return apiResponse;
        });
    }

    private Response send(String pathname, Method method) {
        RequestSpecification specification = RestAssured.given()
                .config(restAssuredConfig)
                .relaxedHTTPSValidation()
                .filter(new AllureRestAssured());

        if (baseUrl != null && !isAbsoluteUrl(pathname)) {
            specification.baseUri(baseUrl);
        }
        if (!headers.isEmpty()) {
            specification.headers(headers);
        }
        if (!cookies.isEmpty()) {
            specification.cookies(cookies);
        }
        if (!queryParams.isEmpty()) {
            specification.queryParams(queryParams);
        }
        if (basicAuth != null) {
            specification.auth().preemptive().basic(basicAuth.username(), basicAuth.password());
        }
        if (contentType != null) {
            specification.contentType(contentType);
        }
        if (formBody) {
            specification.formParams(asMap(body));
        } else if (body != null) {
            specification.body(body);
        }

        return specification.request(method, withRawQuery(pathname));
    }

    private void logRequest(String pathname, Method method) {
        Allure.addAttachment("Request Options", "application/json", toJson(requestLog(pathname, method)), ".json");
        Allure.addAttachment("curl", "text/plain", curl(pathname, method), ".txt");
    }

    private void logResponse(ApiResponse<?> response) {
        Map<String, Object> responseLog = new LinkedHashMap<>();
        responseLog.put("statusCode", response.getStatusCode());
        responseLog.put("headers", response.getHeaders());
        responseLog.put("body", response.getRawBody());
        Allure.addAttachment("Response", "application/json", toJson(responseLog), ".json");
    }

    private Map<String, Object> requestLog(String pathname, Method method) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("method", method.name());
        log.put("baseUrl", baseUrl);
        log.put("pathname", normalizePath(pathname));
        log.put("headers", headers);
        log.put("cookies", cookies);
        log.put("queryParams", queryParams);
        log.put("rawQuery", rawQuery);
        log.put("contentType", contentType == null ? null : contentType.toString());
        log.put("jsonBody", jsonBody);
        log.put("body", body);
        return log;
    }

    private String curl(String pathname, Method method) {
        StringBuilder curl = new StringBuilder("curl");
        if (method != Method.GET) {
            curl.append(" -X ").append(method.name());
        }
        if (basicAuth != null) {
            curl.append(" -u ").append(shellQuote(basicAuth.username() + ":" + basicAuth.password()));
        }
        headers.forEach((name, value) -> curl.append(" -H ").append(shellQuote(name + ": " + value)));
        if (!cookies.isEmpty()) {
            curl.append(" -b ").append(shellQuote(cookieHeader()));
        }
        if (contentType != null) {
            curl.append(" -H ").append(shellQuote("Content-Type: " + contentType));
        }
        if (body != null && !formBody) {
            curl.append(" -d ").append(shellQuote(toJson(body)));
        }
        if (formBody) {
            curl.append(" -d ").append(shellQuote(urlEncoded(asMap(body))));
        }
        curl.append(" ").append(shellQuote(fullUrl(pathname)));
        return curl.toString();
    }

    private String fullUrl(String pathname) {
        String url;
        if (isAbsoluteUrl(pathname)) {
            url = pathname;
        } else {
            String normalizedPath = normalizePath(pathname);
            url = baseUrl == null ? normalizedPath : baseUrl + normalizedPath;
        }

        String queryString = queryString();
        if (!queryString.isBlank()) {
            url += url.contains("?") ? "&" + queryString : "?" + queryString;
        }
        return url;
    }

    private String withRawQuery(String pathname) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return normalizePath(pathname);
        }
        String normalizedPath = normalizePath(pathname);
        return normalizedPath + (normalizedPath.contains("?") ? "&" : "?") + rawQuery;
    }

    private String queryString() {
        String encodedParams = urlEncoded(queryParams);
        if (rawQuery == null || rawQuery.isBlank()) {
            return encodedParams;
        }
        if (encodedParams.isBlank()) {
            return rawQuery;
        }
        return rawQuery + "&" + encodedParams;
    }

    private String urlEncoded(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.entrySet()
                .stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                .collect(Collectors.joining("&"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, ?>) map : Map.of();
    }

    private String cookieHeader() {
        return cookies.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("; "));
    }

    private String stepName(String pathname, Method method) {
        return "HTTP " + method.name() + " " + normalizePath(pathname);
    }

    private String normalizePath(String pathname) {
        if (pathname == null || pathname.isBlank()) {
            return "";
        }
        if (isAbsoluteUrl(pathname)) {
            return pathname;
        }
        return pathname.startsWith("/") ? pathname : "/" + pathname;
    }

    private String removeTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private boolean isAbsoluteUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String toJson(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }
}
