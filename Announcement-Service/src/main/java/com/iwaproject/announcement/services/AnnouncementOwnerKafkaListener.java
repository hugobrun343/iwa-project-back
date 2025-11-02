package com.iwaproject.announcement.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.announcement.dto.AnnouncementOwnerRequest;
import com.iwaproject.announcement.dto.AnnouncementOwnerResponse;
import com.iwaproject.announcement.entities.Announcement;
import com.iwaproject.announcement.repositories.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka listener for announcement owner requests.
 */
@Service
@RequiredArgsConstructor
public class AnnouncementOwnerKafkaListener {

    /**
     * Announcement repository for fetching announcement details.
     */
    private final AnnouncementRepository repository;

    /**
     * Kafka template for sending responses.
     */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * ObjectMapper for JSON deserialization.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Topic name for sending owner responses.
     */
    private static final String RESPONSE_TOPIC = "announcement.owner.response";

    /**
     * Consumes owner requests and sends back responses.
     *
     * @param message the owner request as JSON string
     */
    @KafkaListener(
            topics = "announcement.owner.request",
            groupId = "announcement-service")
    public void consumeOwnerRequest(final String message) {
        try {
            System.out.println("🔸 Received announcement owner request: "
                    + message);

            // Parse JSON string to AnnouncementOwnerRequest
            AnnouncementOwnerRequest request =
                    objectMapper.readValue(message,
                            AnnouncementOwnerRequest.class);

            // Get announcement owner
            String ownerUsername = repository
                    .findById(request.getAnnouncementId())
                    .map(Announcement::getOwnerUsername)
                    .orElse(null);

            AnnouncementOwnerResponse response =
                    new AnnouncementOwnerResponse(
                            request.getRequestId(),
                            request.getAnnouncementId(),
                            ownerUsername);

            kafkaTemplate.send(RESPONSE_TOPIC, response);

            System.out.println("🔹 Sent announcement owner response: "
                    + response);
        } catch (Exception e) {
            System.err.println("❌ Error processing owner request: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }
}
