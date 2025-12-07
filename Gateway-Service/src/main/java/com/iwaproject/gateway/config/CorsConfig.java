package com.iwaproject.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for Spring Cloud Gateway.
 * Configures CORS at the Spring Security WebFlux level.
 */
@Configuration
public class CorsConfig {

    /**
     * Allowed origins from configuration.
     */
    @Value("${cors.allowed.origins:${CORS_GATEWAY_ORIGINS:http://localhost:3000,http://localhost:8085}}")
    private String allowedOrigins;

    /**
     * CORS configuration source for Spring Security WebFlux.
     * This bean configures CORS at the Spring Security level.
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from configuration
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        origins.replaceAll(String::trim);
        
        // Use setAllowedOriginPatterns when allowCredentials is true
        // This is required for Spring Security WebFlux compatibility
        configuration.setAllowedOriginPatterns(origins);
        
        // Allow all common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        
        // Allow common headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Username",
            "X-Email",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Expose headers to client
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Username",
            "X-Email"));
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        // Apply to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}


