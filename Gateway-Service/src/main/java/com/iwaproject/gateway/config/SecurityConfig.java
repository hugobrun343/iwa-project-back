package com.iwaproject.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

/**
 * Reactive Security configuration for Spring Cloud Gateway.
 * Validates JWT tokens from Keycloak and protects routes.
 * Only active in non-test profiles.
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!test")
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/health",
        "/actuator/health",
        "/test",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**"
    };

    /**
     * CORS configuration source bean.
     */
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * Constructor with CORS configuration source.
     *
     * @param corsConfigurationSource the CORS configuration source
     */
    public SecurityConfig(final CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

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
            
            // Enable CORS with configuration source
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Configure authorization rules
            .authorizeExchange(auth -> auth
                // Allow pre-flight CORS requests (OPTIONS)
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                // Public endpoints (no authentication required)
                .pathMatchers(PUBLIC_PATHS).permitAll()
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
