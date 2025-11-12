package com.iwaproject.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for JwtHeadersFilter.
 */
class JwtHeadersFilterTest {

    private JwtHeadersFilter filter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerWebExchange.Builder exchangeBuilder;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpRequest.Builder requestBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtHeadersFilter();
        when(exchange.getRequest()).thenReturn(request);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void testFilterWithJwtAuthentication() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        claims.put("preferred_username", "testuser");
        claims.put("email", "test@example.com");
        claims.put("sub", "user-123");
        claims.put("given_name", "Test");
        claims.put("family_name", "User");

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt);
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(jwtAuth);

        ReflectionTestUtils.setField(filter, "gatewaySecret", "test-secret");

        // Act
        Mono<Void> result = filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(requestBuilder).header("X-Username", "testuser");
        verify(requestBuilder).header("X-Email", "test@example.com");
        verify(requestBuilder).header("X-User-Sub", "user-123");
        verify(requestBuilder).header("X-First-Name", "Test");
        verify(requestBuilder).header("X-Last-Name", "User");
        verify(requestBuilder).header("X-Gateway-Secret", "test-secret");
        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    void testFilterWithJwtAuthenticationNullClaims() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user");  // Minimum required claim

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt);
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(jwtAuth);

        // Act
        Mono<Void> result = filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(requestBuilder).header("X-Username", "");
        verify(requestBuilder).header("X-Email", "");
        verify(requestBuilder).header("X-User-Sub", "user");
        verify(requestBuilder).header("X-First-Name", "");
        verify(requestBuilder).header("X-Last-Name", "");
        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    void testFilterWithoutGatewaySecret() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        claims.put("preferred_username", "testuser");
        claims.put("sub", "user-123");

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt);
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(jwtAuth);

        ReflectionTestUtils.setField(filter, "gatewaySecret", "");

        // Act
        Mono<Void> result = filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(requestBuilder, never()).header(eq("X-Gateway-Secret"), anyString());
        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    void testFilterWithoutJwtAuthentication() {
        // Arrange
        Authentication nonJwtAuth = mock(Authentication.class);
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(nonJwtAuth);

        // Act
        Mono<Void> result = filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(requestBuilder, never()).header(anyString(), anyString());
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testFilterWithoutSecurityContext() {
        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(requestBuilder, never()).header(anyString(), anyString());
        verify(chain).filter(exchange);
    }

    @Test
    void testGetOrder() {
        // Assert
        assertEquals(-90, filter.getOrder());
    }
}
