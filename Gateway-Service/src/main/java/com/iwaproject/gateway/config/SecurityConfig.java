package com.iwaproject.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.
        EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Gateway.
 * Validates JWT tokens from Keycloak and protects routes.
 * Only active in non-test profiles.
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    /**
     * Custom authentication entry point.
     */
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Constructor.
     *
     * @param entryPoint the custom authentication entry point
     */
    public SecurityConfig(
            final CustomAuthenticationEntryPoint entryPoint) {
        this.authenticationEntryPoint = entryPoint;
    }

    /**
     * Configure security filter chain.
     *
     * @param http the HttpSecurity to modify
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http)
            throws Exception {
        http
            // Enable CORS
            .cors(cors -> cors.configure(http))
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers(
                    "/health",
                    "/actuator/health",
                    "/test",
                    // Swagger UI & OpenAPI
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**"
                ).permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(authenticationEntryPoint)
                .jwt(jwt -> {
                    // JWT validation is configured
                    // via application.properties
                })
            );
        return http.build();
    }
}

