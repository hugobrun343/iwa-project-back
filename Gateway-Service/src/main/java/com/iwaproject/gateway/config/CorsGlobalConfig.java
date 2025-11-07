package com.iwaproject.gateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Global CORS configuration for Spring Cloud Gateway.
 * Reactive version using CorsWebFilter.
 */
@Configuration
public class CorsGlobalConfig {

    @Value("${CORS_GATEWAY_ORIGINS:http://localhost:3000}")
    private String corsOrigins;

    /**
     * Configure CORS for reactive gateway.
     *
     * @return CorsWebFilter configured for all routes
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Use single wildcard pattern to allow all origins including null
        // This is necessary for WebSocket connections from file:// or null origins
        corsConfig.addAllowedOriginPattern("*");

        // Allow all common HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Allow all headers
        corsConfig.addAllowedHeader("*");

        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}

