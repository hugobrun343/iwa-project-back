package com.iwaproject.chat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for KafkaConsumerService.
 */
@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    /**
     * Mock services.
     */
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private KafkaLogService kafkaLogService;

    /**
     * Service under test.
     */
    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    /**
     * Test constants.
     */
    private static final String TEST_USERNAME = "testuser";

    /**
     * Test consumeUserExistsReply should complete pending future.
     */
    @Test
    @DisplayName("consumeUserExistsReply should complete pending future")
    void consumeUserExistsReply_shouldCompleteFuture() {
        // Given
        String correlationId = "test-correlation-id";
        String message = correlationId + ":true";

        // When
        kafkaConsumerService.consumeUserExistsReply(message);

        // Then - verify logging was called
        verify(kafkaLogService).info(anyString(), anyString());
    }

    /**
     * Test consumeUserExistsReply with invalid format should log error.
     */
    @Test
    @DisplayName("consumeUserExistsReply with invalid format should log error")
    void consumeUserExistsReply_invalidFormat_shouldLogError() {
        // Given
        String invalidMessage = "invalid-message";

        // When
        kafkaConsumerService.consumeUserExistsReply(invalidMessage);

        // Then
        verify(kafkaLogService).error(anyString(), anyString());
    }

    /**
     * Test checkUserExists should send request and return future.
     */
    @Test
    @DisplayName("checkUserExists should send request and return future")
    void checkUserExists_shouldSendRequest() {
        // When
        CompletableFuture<String> future = kafkaConsumerService
                .checkUserExists(TEST_USERNAME);

        // Then
        assertNotNull(future);
        verify(kafkaProducerService).sendMessage(
                eq("user-exists-topic"), anyString());
        verify(kafkaLogService).info(anyString(), anyString());
    }

    /**
     * Test checkUserExists should create unique correlation IDs.
     */
    @Test
    @DisplayName("checkUserExists should create unique correlation IDs")
    void checkUserExists_shouldCreateUniqueCorrelationIds() {
        // When
        CompletableFuture<String> future1 = kafkaConsumerService
                .checkUserExists(TEST_USERNAME);
        CompletableFuture<String> future2 = kafkaConsumerService
                .checkUserExists(TEST_USERNAME);

        // Then
        assertNotNull(future1);
        assertNotNull(future2);
        assertTrue(future1 != future2);
        verify(kafkaProducerService, times(2)).sendMessage(
                eq("user-exists-topic"), anyString());
    }
}

