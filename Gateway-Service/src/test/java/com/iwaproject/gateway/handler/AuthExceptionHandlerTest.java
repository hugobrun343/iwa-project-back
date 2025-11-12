package com.iwaproject.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for AuthExceptionHandler.
 */
class AuthExceptionHandlerTest {

    private AuthExceptionHandler handler;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private HttpHeaders headers;

    private DataBufferFactory bufferFactory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        handler = new AuthExceptionHandler(objectMapper);
        bufferFactory = new DefaultDataBufferFactory();

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(response.writeWith(any())).thenReturn(Mono.empty());
        
        org.springframework.http.server.RequestPath requestPath = 
            mock(org.springframework.http.server.RequestPath.class);
        when(requestPath.value()).thenReturn("/api/test");
        when(request.getPath()).thenReturn(requestPath);
    }

    @Test
    void testHandleAuthenticationException() {
        // Arrange
        AuthenticationException exception = new AuthenticationException("Invalid credentials") {};

        // Act
        Mono<Void> result = handler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        verify(response).writeWith(any());
    }

    @Test
    void testHandleAccessDeniedException() {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        // Act
        Mono<Void> result = handler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        verify(response).writeWith(any());
    }

    @Test
    void testHandleOtherException() {
        // Arrange
        RuntimeException exception = new RuntimeException("Some other error");

        // Act
        Mono<Void> result = handler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(response, never()).setStatusCode(any());
    }

    @Test
    void testAuthenticationExceptionResponseBody() {
        // Arrange
        AuthenticationException exception = new AuthenticationException("Bad token") {};
        ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);

        // Act
        Mono<Void> result = handler.handle(exchange, exception);
        StepVerifier.create(result).verifyComplete();

        // Assert
        verify(response).writeWith(captor.capture());
        
        Mono<DataBuffer> bufferMono = captor.getValue();
        StepVerifier.create(bufferMono)
                .assertNext(buffer -> {
                    String body = buffer.toString(StandardCharsets.UTF_8);
                    assertTrue(body.contains("\"status\":401"));
                    assertTrue(body.contains("\"error\":\"Authentication Failed\""));
                    assertTrue(body.contains("\"message\":\"Bad token\""));
                    assertTrue(body.contains("\"path\":\"/api/test\""));
                })
                .verifyComplete();
    }

    @Test
    void testAccessDeniedExceptionResponseBody() {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("Insufficient permissions");
        ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);

        // Act
        Mono<Void> result = handler.handle(exchange, exception);
        StepVerifier.create(result).verifyComplete();

        // Assert
        verify(response).writeWith(captor.capture());
        
        Mono<DataBuffer> bufferMono = captor.getValue();
        StepVerifier.create(bufferMono)
                .assertNext(buffer -> {
                    String body = buffer.toString(StandardCharsets.UTF_8);
                    assertTrue(body.contains("\"status\":403"));
                    assertTrue(body.contains("\"error\":\"Access Denied\""));
                    assertTrue(body.contains("\"message\":\"Insufficient permissions\""));
                })
                .verifyComplete();
    }

    @Test
    void testNullPointerExceptionPropagates() {
        // Arrange
        NullPointerException exception = new NullPointerException("Null value");

        // Act & Assert
        StepVerifier.create(handler.handle(exchange, exception))
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    void testAuthenticationExceptionWithNullMessage() {
        // Arrange
        AuthenticationException exception = new AuthenticationException(null) {};

        // Act
        Mono<Void> result = handler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAccessDeniedExceptionWithEmptyMessage() {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("");

        // Act
        Mono<Void> result = handler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }
}
