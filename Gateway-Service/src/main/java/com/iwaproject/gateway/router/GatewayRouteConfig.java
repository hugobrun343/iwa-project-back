package com.iwaproject.gateway.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Additional programmatic route configuration.
 * Complements the main GatewayConfig with advanced routing rules.
 */
@Slf4j
@Configuration
public class GatewayRouteConfig {

    /**
     * Configure additional custom routes with filters.
     * This is an example of more advanced routing configurations.
     *
     * @param builder the route locator builder
     * @return configured RouteLocator
     */
    @Bean
    public RouteLocator customRouteLocator(final RouteLocatorBuilder builder) {
        log.info("Configuring custom gateway routes");

        return builder.routes()
                // Example: Add custom headers to specific routes
                .route("user-with-headers", r -> r
                        .path("/api/users/me")
                        .filters(f -> f
                                .addResponseHeader("X-Response-Source", "Gateway")
                                .removeResponseHeader("Server")
                                .rewritePath("/api/users/(?<segment>.*)",
                                            "/users/${segment}"))
                        .uri("http://user-service:8081"))

                // Example: Rewrite path before forwarding
                .route("api-rewrite", r -> r
                        .path("/external/api/**")
                        .filters(f -> f
                                .rewritePath("/external/api/(?<segment>.*)",
                                            "/api/${segment}"))
                        .uri("http://user-service:8081"))

                .build();
    }
}

