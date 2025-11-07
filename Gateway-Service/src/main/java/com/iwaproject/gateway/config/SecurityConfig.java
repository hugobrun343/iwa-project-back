package com.iwaproject.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;

/**
 * Reactive Security configuration for Spring Cloud Gateway.
 * Validates JWT tokens from Keycloak and protects routes.
 * Only active in non-test profiles.
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!test")
public class SecurityConfig {

    /**
     * Configure reactive security filter chain.
     *
     * @param http the ServerHttpSecurity to modify
     * @return the configured SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            final ServerHttpSecurity http) {

        http
            // Disable CSRF for stateless API
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // Configure authorization rules
            .authorizeExchange(auth -> auth
                // Public endpoints (no authentication required)
                .pathMatchers(
                    "/health",
                    "/actuator/health",
                    "/test",
                    // WebSocket endpoints
                    "/ws",
                    "/ws/**",
                    // Swagger UI & OpenAPI
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**"
                ).permitAll()

                // All other requests require authentication
                .anyExchange().authenticated()
            )

            // Configure OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(
                    new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                .jwt(jwt -> {
                    // JWT validation configured via application.yml
                })
            );

        return http.build();
    }
}
