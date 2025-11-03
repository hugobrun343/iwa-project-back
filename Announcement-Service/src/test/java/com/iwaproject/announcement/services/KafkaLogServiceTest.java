package com.iwaproject.announcement.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for KafkaLogService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaLogService Tests")
class KafkaLogServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaLogService kafkaLogService;

    @BeforeEach
    void setUp() {
        kafkaLogService = new KafkaLogService(
                kafkaTemplate,
                "test-logs-topic",
                "Announcement-Service"
        );
    }

    @Test
    @DisplayName("Should send info log to Kafka")
    void testInfoLog() {
        // Given
        String logger = "TestLogger";
        String message = "This is an info message";

        // When
        kafkaLogService.info(logger, message);

        // Then
        verify(kafkaTemplate).send(anyString(), any());
    }

    @Test
    @DisplayName("Should send error log to Kafka")
    void testErrorLog() {
        // Given
        String logger = "TestLogger";
        String message = "This is an error message";

        // When
        kafkaLogService.error(logger, message);

        // Then
        verify(kafkaTemplate).send(anyString(), any());
    }

    @Test
    @DisplayName("Should send error log with throwable to Kafka")
    void testErrorLogWithThrowable() {
        // Given
        String logger = "TestLogger";
        String message = "This is an error message with exception";
        Throwable throwable = new RuntimeException("Test exception");

        // When
        kafkaLogService.error(logger, message, throwable);

        // Then
        verify(kafkaTemplate).send(anyString(), any());
    }

    @Test
    @DisplayName("Should send debug log to Kafka")
    void testDebugLog() {
        // Given
        String logger = "TestLogger";
        String message = "This is a debug message";

        // When
        kafkaLogService.debug(logger, message);

        // Then
        verify(kafkaTemplate).send(anyString(), any());
    }
}
