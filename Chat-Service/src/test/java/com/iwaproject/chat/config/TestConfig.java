package com.iwaproject.chat.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration to mock external dependencies.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    // Mock any external services that might be needed for Chat-Service tests
    // Add @MockBean annotations here as needed
}
