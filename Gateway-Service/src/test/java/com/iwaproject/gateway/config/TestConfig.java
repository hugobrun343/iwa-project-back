package com.iwaproject.gateway.config;

import org.mockito.InjectMocks;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration to mock external dependencies.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Mock RestTemplate for tests.
     */
    @InjectMocks
    private org.springframework.web.client.RestTemplate restTemplate;
}
