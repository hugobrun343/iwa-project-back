package com.iwaproject.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter that extracts JWT claims and adds them as headers for downstream services.
 * This allows backend services to identify the authenticated user.
 */
@Slf4j
@Component
public class JwtHeadersFilter implements GlobalFilter, Ordered {

    /**
     * Gateway secret for service-to-service authentication.
     */
    @Value("${gateway.secret:}")
    private String gatewaySecret;

    /**
     * Extract JWT claims and add headers to the request.
     *
     * @param exchange the current server exchange
     * @param chain provides a way to delegate to the next filter
     * @return Mono<Void> to indicate when request processing is complete
     */
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(securityContext -> {
                if (securityContext.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                    Jwt jwt = jwtAuth.getToken();
                    
                    // Extract claims from JWT
                    String username = jwt.getClaimAsString("preferred_username");
                    String email = jwt.getClaimAsString("email");
                    String sub = jwt.getClaimAsString("sub");
                    String firstName = jwt.getClaimAsString("given_name");
                    String lastName = jwt.getClaimAsString("family_name");
                    
                    log.debug("Extracting JWT claims - Username: {}, Email: {}, Sub: {}", 
                             username, email, sub);
                    
                    // Build the request with JWT claims and gateway secret
                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                        .header("X-Username", username != null ? username : "")
                        .header("X-Email", email != null ? email : "")
                        .header("X-User-Sub", sub != null ? sub : "")
                        .header("X-First-Name", firstName != null ? firstName : "")
                        .header("X-Last-Name", lastName != null ? lastName : "");
                    
                    // Add gateway secret if configured
                    if (gatewaySecret != null && !gatewaySecret.isEmpty()) {
                        requestBuilder.header("X-Gateway-Secret", gatewaySecret);
                        log.debug("Added X-Gateway-Secret header");
                    }
                    
                    ServerHttpRequest request = requestBuilder.build();
                    
                    return chain.filter(exchange.mutate().request(request).build());
                }
                
                // No JWT authentication, continue without adding headers
                return chain.filter(exchange);
            })
            .switchIfEmpty(chain.filter(exchange));
    }

    /**
     * Set the order for this filter.
     * Should run after JWT authentication but before routing.
     *
     * @return order value
     */
    @Override
    public int getOrder() {
        return -90; // After JwtAuthenticationFilter (-100) but before routing
    }
}
