package com.pulsebackend.utils;

import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WaitTest {
    @Test
    public void waitingForShouldReturnFirstNonNullValue() {
        AtomicInteger attempts = new AtomicInteger();

        String result = new Wait(1_000, 10).waitingFor(() ->
                attempts.incrementAndGet() < 3 ? null : "ready"
        );

        assertEquals(result, "ready");
        assertTrue(attempts.get() >= 3);
    }

    @Test
    public void waitingForShouldSupportFunctionArguments() {
        String result = new Wait(1_000, 10).waitingFor(String::toUpperCase, "pulse");

        assertEquals(result, "PULSE");
    }
}
