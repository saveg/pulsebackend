package com.pulsebackend.components.request;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class LogRequestTest {
    private HttpServer server;
    private String baseUrl;

    @BeforeMethod
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/health", exchange -> respond(exchange, 200, "{\"status\":\"UP\"}"));
        server.createContext("/echo", exchange -> respond(exchange, 201, "{\"id\":\"42\",\"name\":\"pulse\"}"));
        server.createContext("/not-found", exchange -> respond(exchange, 404, "{\"message\":\"not found\"}"));
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterMethod(alwaysRun = true)
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void shouldDeserializeSuccessfulResponseToPojo() {
        ApiResponse<HealthBody> response = new LogRequest()
                .baseUrl(baseUrl)
                .header("source", "autotest")
                .queryParam("verbose", true)
                .get("/health", HealthBody.class);

        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getBody().status(), "UP");
        assertEquals(response.bodyAs(HealthBody.class).status(), "UP");
    }

    @Test
    public void shouldSendJsonBodyAndDeserializeCreatedResponse() {
        ApiResponse<EchoBody> response = new LogRequest(ApiControllerOptions.builder()
                .baseUrl(baseUrl)
                .bearerToken("token")
                .cookie("session", "abc")
                .build())
                .json(Map.of("name", "pulse"))
                .post("/echo", EchoBody.class);

        assertEquals(response.getStatusCode(), 201);
        assertEquals(response.getBody().id(), "42");
        assertEquals(response.getBody().name(), "pulse");
    }

    @Test
    public void shouldSeparateSuccessAndErrorRequests() {
        assertThrows(
                AssertionError.class,
                () -> new LogRequest().baseUrl(baseUrl).get("/not-found")
        );

        ApiResponse<ErrorBody> response = new LogRequest()
                .baseUrl(baseUrl)
                .getError("/not-found", ErrorBody.class);

        assertEquals(response.getStatusCode(), 404);
        assertEquals(response.getBody().message(), "not found");
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public record HealthBody(String status) {
    }

    public record EchoBody(String id, String name) {
    }

    public record ErrorBody(String message) {
    }
}
