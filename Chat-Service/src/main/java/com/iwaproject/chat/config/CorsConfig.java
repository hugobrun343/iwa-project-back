package com.iwaproject.chat.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for Chat Service.
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
        List<String> directOrigins = new ArrayList<>();
        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(directOrigins::add);

        if (!directOrigins.contains("null")) {
            directOrigins.add("null");
        }

        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(final HttpServletRequest request) {
                String requestUri = request.getRequestURI();
                
                // For WebSocket endpoints, always return CORS config (needed for SockJS handshake)
                if (requestUri != null && requestUri.startsWith("/ws")) {
                    CorsConfiguration configuration = new CorsConfiguration();
                    configuration.setAllowedOriginPatterns(List.of("*"));
                    configuration.setAllowedMethods(Arrays.asList(
                            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
                    ));
                    configuration.setAllowedHeaders(List.of("*"));
                    configuration.setAllowCredentials(true);
                    configuration.setMaxAge(PREFLIGHT_MAX_AGE);
                    return configuration;
                }
                
                // If the call already comes through the gateway, let it manage CORS headers
                if (StringUtils.hasText(request.getHeader("X-Gateway-Secret"))) {
                    return null;
                }

                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(directOrigins);
                configuration.setAllowedMethods(Arrays.asList(
                        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
                ));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(PREFLIGHT_MAX_AGE);
                return configuration;
            }
        };
    }
}

