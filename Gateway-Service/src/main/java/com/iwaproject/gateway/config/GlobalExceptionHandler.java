package com.iwaproject.gateway.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler for reactive gateway.
 * Handles all exceptions and formats error responses.
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalExceptionHandler extends DefaultErrorAttributes {

    /**
     * Customize error attributes for all exceptions.
     *
     * @param request the server request
     * @param options the error attribute options
     * @return customized error attributes map
     */
    @Override
    public Map<String, Object> getErrorAttributes(
            final ServerRequest request,
            final ErrorAttributeOptions options) {

        Throwable error = getError(request);
        Map<String, Object> errorAttributes = new HashMap<>();

        HttpStatus status = determineHttpStatus(error);

        errorAttributes.put("timestamp", LocalDateTime.now().toString());
        errorAttributes.put("path", request.path());
        errorAttributes.put("status", status.value());
        errorAttributes.put("error", status.getReasonPhrase());
        errorAttributes.put("message", determineMessage(error));

        log.error("Global error handler caught exception on path {}: {}",
                request.path(), error.getMessage(), error);

        return errorAttributes;
    }

    /**
     * Determine the HTTP status from the exception.
     *
     * @param error the error
     * @return the HTTP status
     */
    private HttpStatus determineHttpStatus(final Throwable error) {
        if (error instanceof ResponseStatusException) {
            return HttpStatus.valueOf(
                    ((ResponseStatusException) error).getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Determine the error message.
     *
     * @param error the error
     * @return the error message
     */
    private String determineMessage(final Throwable error) {
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return "An unexpected error occurred";
    }
}
