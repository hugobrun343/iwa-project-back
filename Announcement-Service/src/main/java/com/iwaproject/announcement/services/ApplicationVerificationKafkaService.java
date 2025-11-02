package com.iwaproject.announcement.services;

import com.iwaproject.announcement.dto.ApplicationVerificationRequest;
import com.iwaproject.announcement.dto.ApplicationVerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling asynchronous application verification via Kafka.
 */
@Service
@RequiredArgsConstructor
public class ApplicationVerificationKafkaService {

    /**
     * Kafka template for sending messages.
     */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Topic name for sending verification requests.
     */
    private static final String REQUEST_TOPIC = "application.verify.request";

    /**
     * Timeout for verification requests in seconds.
     */
    private static final int TIMEOUT_SECONDS = 5;

    /**
     * Store pending requests until responses arrive.
     */
    private final ConcurrentMap<String, CompletableFuture<Boolean>>
            pendingRequests = new ConcurrentHashMap<>();

    /**
     * Sends verification request asynchronously and returns a future
     * that will be completed when a response is received.
     *
     * @param username the username to verify
     * @param announcementId the announcement ID
     * @return CompletableFuture with verification result
     */
    public CompletableFuture<Boolean> hasUserAcceptedApplication(
            final String username,
            final Long announcementId) {
        String requestId = UUID.randomUUID().toString();
        ApplicationVerificationRequest request =
                new ApplicationVerificationRequest(
                        requestId, username, announcementId);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        kafkaTemplate.send(REQUEST_TOPIC, request);

        System.out.println("🔹 Sent verification request: "
                + request);

        // Optional: add timeout safeguard
        CompletableFuture.delayedExecutor(TIMEOUT_SECONDS,
                TimeUnit.SECONDS).execute(() -> {
                    if (!future.isDone()) {
                        future.complete(false);
                        pendingRequests.remove(requestId);
                        System.err.println(
                                "⚠️ Verification timeout for requestId "
                                        + requestId);
                    }
                });

        return future;
    }

    /**
     * Called by Kafka listener when a response arrives.
     *
     * @param response the verification response
     */
    public void handleResponse(
            final ApplicationVerificationResponse response) {
        CompletableFuture<Boolean> future =
                pendingRequests.remove(response.getRequestId());
        if (future != null) {
            future.complete(response.isAccepted());
            System.out.println("✅ Received response for "
                    + response.getRequestId()
                    + ": accepted=" + response.isAccepted());
        } else {
            System.err.println("⚠️ No pending request for "
                    + response.getRequestId());
        }
    }
}
