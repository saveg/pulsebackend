package com.pulsebackend;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SmokeTest {
    @Test
    public void projectShouldRunTestNgTests() {
        assertEquals(Application.name(), "PulseBackend");
    }
}
