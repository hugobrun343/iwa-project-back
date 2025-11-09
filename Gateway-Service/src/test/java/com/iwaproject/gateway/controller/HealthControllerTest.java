package com.iwaproject.gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HealthController.
 */
class HealthControllerTest {

    private HealthController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthController();
    }

    @Test
    void testHealthEndpoint() {
        // Act
        Map<String, Object> response = controller.health();

        // Assert
        assertNotNull(response);
        assertEquals("UP", response.get("status"));
        assertEquals("Gateway Service", response.get("service"));
        assertEquals("1.0.0", response.get("version"));
        assertEquals("Gateway is running successfully!", response.get("message"));
        assertNotNull(response.get("timestamp"));
    }

    @Test
    void testHealthEndpointContainsAllRequiredFields() {
        // Act
        Map<String, Object> response = controller.health();

        // Assert
        assertTrue(response.containsKey("status"));
        assertTrue(response.containsKey("service"));
        assertTrue(response.containsKey("timestamp"));
        assertTrue(response.containsKey("version"));
        assertTrue(response.containsKey("message"));
    }

    @Test
    void testTestDeploymentEndpoint() {
        // Act
        Map<String, Object> response = controller.testdecon();

        // Assert
        assertNotNull(response);
        assertEquals("Hello World !!!!", response.get("message"));
        assertEquals("Success", response.get("deployment"));
        assertNotNull(response.get("timestamp"));
    }

    @Test
    void testTestDeploymentEndpointContainsAllFields() {
        // Act
        Map<String, Object> response = controller.testdecon();

        // Assert
        assertTrue(response.containsKey("message"));
        assertTrue(response.containsKey("timestamp"));
        assertTrue(response.containsKey("deployment"));
        assertEquals(3, response.size());
    }

    @Test
    void testHealthEndpointMultipleCalls() throws InterruptedException {
        // Act
        Map<String, Object> response1 = controller.health();
        Thread.sleep(10); // Small delay to ensure different timestamps
        Map<String, Object> response2 = controller.health();

        // Assert - Both calls should succeed
        assertEquals("UP", response1.get("status"));
        assertEquals("UP", response2.get("status"));
        assertNotEquals(response1.get("timestamp"), response2.get("timestamp"));
    }
}
