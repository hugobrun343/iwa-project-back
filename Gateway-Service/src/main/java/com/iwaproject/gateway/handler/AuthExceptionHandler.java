package com.iwaproject.gateway.handler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Handler for authentication and authorization exceptions.
 * Reactive version for Spring Cloud Gateway.
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class AuthExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    /**
     * Handle authentication and authorization exceptions.
     *
     * @param exchange the current server exchange
     * @param ex the exception to handle
     * @return a Mono signaling completion
     */
    @Override
    public Mono<Void> handle(final ServerWebExchange exchange,
                            final Throwable ex) {
        if (ex instanceof AuthenticationException) {
            return handleAuthenticationException(exchange,
                    (AuthenticationException) ex);
        } else if (ex instanceof AccessDeniedException) {
            return handleAccessDeniedException(exchange,
                    (AccessDeniedException) ex);
        }

        // Not an auth-related exception, let other handlers deal with it
        return Mono.error(ex);
    }

    /**
     * Handle authentication exceptions (401 Unauthorized).
     *
     * @param exchange the server exchange
     * @param ex the authentication exception
     * @return a Mono signaling completion
     */
    private Mono<Void> handleAuthenticationException(
            final ServerWebExchange exchange,
            final AuthenticationException ex) {

        log.warn("Authentication failed: {}", ex.getMessage());

        return writeErrorResponse(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                ex.getMessage()
        );
    }

    /**
     * Handle access denied exceptions (403 Forbidden).
     *
     * @param exchange the server exchange
     * @param ex the access denied exception
     * @return a Mono signaling completion
     */
    private Mono<Void> handleAccessDeniedException(
            final ServerWebExchange exchange,
            final AccessDeniedException ex) {

        log.warn("Access denied: {}", ex.getMessage());

        return writeErrorResponse(
                exchange,
                HttpStatus.FORBIDDEN,
                "Access Denied",
                ex.getMessage()
        );
    }

    /**
     * Write error response to client.
     *
     * @param exchange the server exchange
     * @param status the HTTP status
     * @param error the error type
     * @param message the error message
     * @return a Mono signaling completion
     */
    private Mono<Void> writeErrorResponse(
            final ServerWebExchange exchange,
            final HttpStatus status,
            final String error,
            final String message) {

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", LocalDateTime.now().toString());
        errorBody.put("status", status.value());
        errorBody.put("error", error);
        errorBody.put("message", message);
        errorBody.put("path", exchange.getRequest().getPath().value());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorBody);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing error response", e);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(("Error: " + message).getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
}

