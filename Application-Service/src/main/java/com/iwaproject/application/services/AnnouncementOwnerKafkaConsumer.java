package com.iwaproject.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.application.dtos.AnnouncementOwnerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for announcement owner responses.
 */
@Service
@RequiredArgsConstructor
public class AnnouncementOwnerKafkaConsumer {

    /**
     * Service for handling owner responses.
     */
    private final AnnouncementOwnerKafkaService kafkaService;

    /**
     * ObjectMapper for JSON deserialization.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Consumes announcement owner responses.
     *
     * @param message the owner response as JSON string
     */
    @KafkaListener(
            topics = "announcement.owner.response",
            groupId = "application-service")
    public void consumeResponse(final String message) {
        try {
            // Parse JSON string to AnnouncementOwnerResponse
            AnnouncementOwnerResponse response =
                    objectMapper.readValue(message,
                            AnnouncementOwnerResponse.class);

            kafkaService.handleResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
