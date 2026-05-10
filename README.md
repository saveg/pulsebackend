# PulseBackend

Maven/TestNG backend test project.

## Run tests

```bash
mvn test
```

Optional runtime properties:

```bash
mvn test -Denv=local -DconfigFile=application.local.yaml
```

## API clients

Rest Assured layer lives in:

- `src/main/java/com/pulsebackend/components/request` - request options, fluent request builder, typed response.
- `src/main/java/com/pulsebackend/base/BaseApiClient.java` - base client for shared request options.
- `src/main/java/com/pulsebackend/controllers/api` - API controllers.
- `src/main/java/com/pulsebackend/clients/api/PulseApiClient.java` - client that exposes controllers.

Example controller method:

```java
@Step("Create transaction")
public ApiResponse<CreateTransactionResponse> create(CreateTransactionRequest body, String source) {
    return request()
            .json(body)
            .header("source", source)
            .post("/api/v1/transaction/calculate", CreateTransactionResponse.class);
}
```

Example usage:

```java
PulseApiClient api = new PulseApiClient(ApiControllerOptions.builder()
        .baseUrl("http://localhost:8080")
        .bearerToken(token)
        .build());

HealthController.HealthResponse body = api.health.getHealth().getBody();
```

Each request is wrapped in an Allure step and attaches request options, curl, Rest Assured request/response logs, and a typed `ApiResponse<T>`.
