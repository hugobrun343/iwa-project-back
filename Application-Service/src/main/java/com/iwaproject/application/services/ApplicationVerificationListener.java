package com.iwaproject.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.application.dtos.ApplicationVerificationRequest;
import com.iwaproject.application.dtos.ApplicationVerificationResponse;
import com.iwaproject.application.entities.ApplicationStatus;
import com.iwaproject.application.repositories.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka listener for application verification requests.
 */
@Service
@RequiredArgsConstructor
public class ApplicationVerificationListener {

    /**
     * Application repository for checking application status.
     */
    private final ApplicationRepository repository;

    /**
     * Kafka template for sending responses.
     */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * ObjectMapper for JSON deserialization.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Topic name for sending verification responses.
     */
    private static final String RESPONSE_TOPIC = "application.verify.response";

    /**
     * Consumes verification requests and sends back responses.
     *
     * @param message the verification request as String
     */
    @KafkaListener(
            topics = "application.verify.request",
            groupId = "application-service")
    public void consumeVerificationRequest(final String message) {
        try {
            System.out.println("🔸 Received verification request: "
                    + message);

            // Parse JSON string to ApplicationVerificationRequest
            ApplicationVerificationRequest request =
                    objectMapper.readValue(message,
                            ApplicationVerificationRequest.class);

            // Check if user has an accepted application for announcement
            boolean accepted = repository
                    .existsByGuardianUsernameAndAnnouncementIdAndStatus(
                            request.getUsername(),
                            request.getAnnouncementId().intValue(),
                            ApplicationStatus.ACCEPTED);

            ApplicationVerificationResponse response =
                    new ApplicationVerificationResponse(
                            request.getRequestId(),
                            request.getUsername(),
                            request.getAnnouncementId(),
                            accepted);

            kafkaTemplate.send(RESPONSE_TOPIC, response);

            System.out.println("🔹 Sent verification response: " + response);
        } catch (Exception e) {
            System.err.println("❌ Error processing verification request: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }
}
