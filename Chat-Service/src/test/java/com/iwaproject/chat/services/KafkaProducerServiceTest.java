package com.iwaproject.chat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for KafkaProducerService.
 */
@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    /**
     * Mocked Kafka template.
     */
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Service under test.
     */
    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    /**
     * Capture for System.out.
     */
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    /**
     * Verify that sendMessage delegates to KafkaTemplate and writes to stdout.
     */
    @Test
    @DisplayName("sendMessage should send to Kafka and print")
    void sendMessage_shouldSendToKafkaAndPrint() {
        // Given
        String topic = "test-topic";
        String message = "hello";
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq(topic), eq(message))).thenReturn(future);

        // Redirect stdout
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        kafkaProducerService.sendMessage(topic, message);

        // Then
        verify(kafkaTemplate).send(eq(topic), eq(message));
        assertThat(outContent.toString())
                .contains("Message sent to topic " + topic + ": " + message);

        // restore
        System.setOut(originalOut);
    }
}

