package com.iwaproject.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for KafkaLogService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaLogService Tests")
class KafkaLogServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Captor
    private ArgumentCaptor<Map<String, Object>> logEntryCaptor;

    private KafkaLogService kafkaLogService;

    private static final String LOGS_TOPIC = "test-logs";
    private static final String SERVICE_NAME = "Test-Service";

    @BeforeEach
    void setUp() {
        kafkaLogService = new KafkaLogService(
                kafkaTemplate,
                LOGS_TOPIC,
                SERVICE_NAME
        );
    }

    @Test
    @DisplayName("Should send INFO log to Kafka")
    void testInfo() {
        // When
        kafkaLogService.info("TestLogger", "Test info message");

        // Then
        verify(kafkaTemplate).send(eq(LOGS_TOPIC), logEntryCaptor.capture());
        Map<String, Object> logEntry = logEntryCaptor.getValue();

        assertEquals("INFO", logEntry.get("level"));
        assertEquals(SERVICE_NAME, logEntry.get("service"));
        assertEquals("TestLogger", logEntry.get("logger"));
        assertEquals("Test info message", logEntry.get("message"));
        assertNotNull(logEntry.get("timestamp"));
        assertNull(logEntry.get("exception"));
    }

    @Test
    @DisplayName("Should send ERROR log to Kafka")
    void testError() {
        // When
        kafkaLogService.error("TestLogger", "Test error message");

        // Then
        verify(kafkaTemplate).send(eq(LOGS_TOPIC), logEntryCaptor.capture());
        Map<String, Object> logEntry = logEntryCaptor.getValue();

        assertEquals("ERROR", logEntry.get("level"));
        assertEquals("Test error message", logEntry.get("message"));
    }

    @Test
    @DisplayName("Should send ERROR log with exception to Kafka")
    void testErrorWithException() {
        // Given
        Exception exception = new RuntimeException("Test exception");

        // When
        kafkaLogService.error("TestLogger", "Error with exception", exception);

        // Then
        verify(kafkaTemplate).send(eq(LOGS_TOPIC), logEntryCaptor.capture());
        Map<String, Object> logEntry = logEntryCaptor.getValue();

        assertEquals("ERROR", logEntry.get("level"));
        assertEquals("Error with exception", logEntry.get("message"));
        assertNotNull(logEntry.get("exception"));
        assertTrue(((String) logEntry.get("exception")).contains("RuntimeException"));
        assertTrue(((String) logEntry.get("exception")).contains("Test exception"));
    }

    @Test
    @DisplayName("Should send DEBUG log to Kafka")
    void testDebug() {
        // When
        kafkaLogService.debug("TestLogger", "Test debug message");

        // Then
        verify(kafkaTemplate).send(eq(LOGS_TOPIC), logEntryCaptor.capture());
        Map<String, Object> logEntry = logEntryCaptor.getValue();

        assertEquals("DEBUG", logEntry.get("level"));
        assertEquals("Test debug message", logEntry.get("message"));
    }

    @Test
    @DisplayName("Should send WARN log to Kafka")
    void testWarn() {
        // When
        kafkaLogService.warn("TestLogger", "Test warn message");

        // Then
        verify(kafkaTemplate).send(eq(LOGS_TOPIC), logEntryCaptor.capture());
        Map<String, Object> logEntry = logEntryCaptor.getValue();

        assertEquals("WARN", logEntry.get("level"));
        assertEquals("Test warn message", logEntry.get("message"));
    }

    @Test
    @DisplayName("Should handle Kafka send failure gracefully")
    void testKafkaFailure() {
        // Given
        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaTemplate).send(anyString(), any());

        // When/Then - should not throw exception
        assertDoesNotThrow(() ->
                kafkaLogService.info("TestLogger", "Test message")
        );

        verify(kafkaTemplate).send(eq(LOGS_TOPIC), any());
    }

    @Test
    @DisplayName("Should include all log fields")
    void testLogFieldsComplete() {
        // When
        kafkaLogService.info("com.test.MyClass", "Complete log entry");

        // Then
        verify(kafkaTemplate).send(eq(LOGS_TOPIC), logEntryCaptor.capture());
        Map<String, Object> logEntry = logEntryCaptor.getValue();

        assertTrue(logEntry.containsKey("level"));
        assertTrue(logEntry.containsKey("service"));
        assertTrue(logEntry.containsKey("logger"));
        assertTrue(logEntry.containsKey("message"));
        assertTrue(logEntry.containsKey("timestamp"));
        assertEquals(5, logEntry.size()); // Should only have these 5 fields for non-error logs
    }

    @Test
    @DisplayName("Should include exception field when throwable provided")
    void testExceptionField() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // When
        kafkaLogService.error("TestLogger", "Error occurred", exception);

        // Then
        verify(kafkaTemplate).send(eq(LOGS_TOPIC), logEntryCaptor.capture());
        Map<String, Object> logEntry = logEntryCaptor.getValue();

        assertTrue(logEntry.containsKey("exception"));
        String exceptionStr = (String) logEntry.get("exception");
        assertTrue(exceptionStr.contains("IllegalArgumentException"));
        assertTrue(exceptionStr.contains("Invalid argument"));
    }
}
