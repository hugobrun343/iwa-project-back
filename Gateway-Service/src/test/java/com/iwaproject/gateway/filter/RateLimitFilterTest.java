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

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests for RateLimitFilter.
 */
class RateLimitFilterTest {

    private RateLimitFilter filter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private HttpHeaders requestHeaders;

    @Mock
    private HttpHeaders responseHeaders;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new RateLimitFilter();
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getHeaders()).thenReturn(requestHeaders);
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(response.setComplete()).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void testFirstRequestAllowed() {
        // Arrange
        when(request.getRemoteAddress())
                .thenReturn(new InetSocketAddress("192.168.1.1", 8080));
        when(requestHeaders.getFirst("X-Forwarded-For")).thenReturn(null);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
        verify(response, never()).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void testXForwardedForHeader() {
        // Arrange
        when(requestHeaders.getFirst("X-Forwarded-For"))
                .thenReturn("203.0.113.1, 198.51.100.1");

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testMultipleRequestsWithinLimit() {
        // Arrange
        when(request.getRemoteAddress())
                .thenReturn(new InetSocketAddress("10.0.0.1", 8080));
        when(requestHeaders.getFirst("X-Forwarded-For")).thenReturn(null);

        // Act - Make 10 requests
        for (int i = 0; i < 10; i++) {
            Mono<Void> result = filter.filter(exchange, chain);
            StepVerifier.create(result).verifyComplete();
        }

        // Assert
        verify(chain, times(10)).filter(exchange);
        verify(response, never()).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void testRateLimitExceeded() {
        // Arrange
        when(request.getRemoteAddress())
                .thenReturn(new InetSocketAddress("172.16.0.1", 8080));
        when(requestHeaders.getFirst("X-Forwarded-For")).thenReturn(null);

        // Act - Make 101 requests (exceeding the limit of 100)
        for (int i = 0; i < 100; i++) {
            Mono<Void> result = filter.filter(exchange, chain);
            StepVerifier.create(result).verifyComplete();
        }

        // The 101st request should be rejected
        Mono<Void> result = filter.filter(exchange, chain);
        StepVerifier.create(result).verifyComplete();

        // Assert
        verify(response, atLeastOnce()).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        verify(responseHeaders, atLeastOnce())
                .add("X-Rate-Limit-Retry-After-Seconds", "60");
    }

    @Test
    void testUnknownClientId() {
        // Arrange
        when(request.getRemoteAddress()).thenReturn(null);
        when(requestHeaders.getFirst("X-Forwarded-For")).thenReturn(null);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testGetOrder() {
        // Assert
        assertEquals(-50, filter.getOrder());
    }

    @Test
    void testDifferentClientsIndependentLimits() {
        // Arrange - First client
        when(request.getRemoteAddress())
                .thenReturn(new InetSocketAddress("192.168.1.100", 8080));
        when(requestHeaders.getFirst("X-Forwarded-For")).thenReturn(null);

        // Act - First client makes requests
        for (int i = 0; i < 50; i++) {
            Mono<Void> result = filter.filter(exchange, chain);
            StepVerifier.create(result).verifyComplete();
        }

        // Arrange - Second client
        when(request.getRemoteAddress())
                .thenReturn(new InetSocketAddress("192.168.1.200", 8080));

        // Act - Second client makes requests
        for (int i = 0; i < 50; i++) {
            Mono<Void> result = filter.filter(exchange, chain);
            StepVerifier.create(result).verifyComplete();
        }

        // Assert - Both clients should be allowed
        verify(chain, times(100)).filter(exchange);
        verify(response, never()).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    }
}
