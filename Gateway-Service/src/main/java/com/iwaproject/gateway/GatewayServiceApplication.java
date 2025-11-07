package com.iwaproject.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;

/**
 * Main application class for Gateway Service.
 */
@SpringBootApplication(exclude = {ReactiveOAuth2ResourceServerAutoConfiguration.class})
public class GatewayServiceApplication {

    /**
     * Protected constructor for Spring Boot.
     */
    protected GatewayServiceApplication() {
        // Spring Boot needs to instantiate this class
    }

    /**
     * Main method to start the Gateway Service application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        // Gateway Service - Updated for deployment test
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

}
