package com.iwaproject.favorite.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for Favorite Service.
 * Only allows requests from the Gateway.
 */
@Configuration
public class CorsConfig {

    /**
     * Allowed CORS origins (Gateway only).
     */
    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    /**
     * Cache preflight duration constant.
     */
    private static final long PREFLIGHT_MAX_AGE = 3600L;

    /**
     * Configure CORS to accept only Gateway requests.
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Only allow Gateway origin
        configuration.setAllowedOrigins(
                Arrays.asList(allowedOrigins.split(","))
        );

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allow all headers (including X-Username, X-Gateway-Secret)
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Cache preflight response
        configuration.setMaxAge(PREFLIGHT_MAX_AGE);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
