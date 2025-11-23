package com.iwaproject.gateway.filter;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT Authentication Filter for Spring Cloud Gateway.
 * Validates JWT tokens on incoming requests.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/health",
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/api/payments/config"
    );

    /**
     * Filter incoming requests to validate JWT tokens.
     *
     * @param exchange the current server exchange
     * @param chain the gateway filter chain
     * @return a Mono signaling completion
     */
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange,
                             final GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip JWT validation for OPTIONS requests (CORS preflight)
        if (exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            log.debug("Skipping JWT validation for OPTIONS request: {}", path);
            return chain.filter(exchange);
        }

        // Skip JWT validation for public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}",
                    path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Token validation is delegated to Spring Security OAuth2
        // Resource Server configuration
        log.debug("JWT token found for path: {}", path);
        return chain.filter(exchange);
    }

    /**
     * Determine filter order (run early in the chain).
     *
     * @return the order value
     */
    @Override
    public int getOrder() {
        return -100; // High priority
    }

    /**
     * Check if the path is public (doesn't require authentication).
     *
     * @param path the request path
     * @return true if public, false otherwise
     */
    private boolean isPublicPath(final String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(path::startsWith);
    }
}

