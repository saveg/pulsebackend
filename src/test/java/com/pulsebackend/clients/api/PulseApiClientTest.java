package com.pulsebackend.clients.api;

import com.pulsebackend.components.request.ApiControllerOptions;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class PulseApiClientTest {
    @Test
    public void shouldExposeApiControllersWithSharedOptions() {
        ApiControllerOptions options = ApiControllerOptions.builder()
                .baseUrl("http://localhost:9000")
                .header("source", "autotest")
                .build();

        PulseApiClient client = new PulseApiClient(options);

        assertNotNull(client.health);
        assertNotNull(client.testUtils);
        assertEquals(client.getRequestOptions().getBaseUrl(), "http://localhost:9000");
        assertEquals(client.health.getRequestOptions().getHeaders().get("source"), "autotest");
        assertEquals(client.testUtils.getRequestOptions().getHeaders().get("source"), "autotest");
    }
}
