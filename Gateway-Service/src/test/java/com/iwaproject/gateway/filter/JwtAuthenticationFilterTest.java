package com.iwaproject.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests for JwtAuthenticationFilter.
 */
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtAuthenticationFilter();
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(response.setComplete()).thenReturn(Mono.empty());
    }

    @Test
    void testPublicPathHealth() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("http://localhost/health"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void testPublicPathActuatorHealth() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("http://localhost/actuator/health"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testPublicPathSwagger() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("http://localhost/swagger-ui/index.html"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testMissingAuthorizationHeader() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("http://localhost/api/users"));
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).setComplete();
        verify(chain, never()).filter(exchange);
    }

    @Test
    void testInvalidAuthorizationHeader() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("http://localhost/api/users"));
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Basic abc123");

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).setComplete();
        verify(chain, never()).filter(exchange);
    }

    @Test
    void testValidBearerToken() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("http://localhost/api/users"));
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
        verify(response, never()).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testGetOrder() {
        // Assert
        assertEquals(-100, filter.getOrder());
    }
}
