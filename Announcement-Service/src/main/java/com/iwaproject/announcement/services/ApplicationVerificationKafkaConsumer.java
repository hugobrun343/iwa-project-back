package com.iwaproject.announcement.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.announcement.dto.ApplicationVerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for application verification responses.
 */
@Service
@RequiredArgsConstructor
public class ApplicationVerificationKafkaConsumer {

    /**
     * Service for handling verification responses.
     */
    private final ApplicationVerificationKafkaService kafkaService;

    /**
     * ObjectMapper for JSON deserialization.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Consumes application verification responses.
     *
     * @param message the verification response as JSON string
     */
    @KafkaListener(
            topics = "application.verify.response",
            groupId = "announcement-service")
    public void consumeResponse(final String message) {
        try {
            // Parse JSON string to ApplicationVerificationResponse
            ApplicationVerificationResponse response =
                    objectMapper.readValue(message,
                            ApplicationVerificationResponse.class);

            kafkaService.handleResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
