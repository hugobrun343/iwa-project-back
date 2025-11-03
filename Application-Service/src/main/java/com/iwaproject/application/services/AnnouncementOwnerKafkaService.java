package com.iwaproject.application.services;

import com.iwaproject.application.dtos.AnnouncementOwnerRequest;
import com.iwaproject.application.dtos.AnnouncementOwnerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling announcement owner requests via Kafka.
 */
@Service
@RequiredArgsConstructor
public class AnnouncementOwnerKafkaService {

    /**
     * Kafka template for sending messages.
     */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Topic name for sending owner requests.
     */
    private static final String REQUEST_TOPIC = "announcement.owner.request";

    /**
     * Timeout for requests in seconds.
     */
    private static final int TIMEOUT_SECONDS = 5;

    /**
     * Store pending requests until responses arrive.
     */
    private final ConcurrentMap<String, CompletableFuture<String>>
            pendingRequests = new ConcurrentHashMap<>();

    /**
     * Sends owner request asynchronously and returns a future
     * that will be completed when a response is received.
     *
     * @param announcementId the announcement ID
     * @return CompletableFuture with owner username
     */
    public CompletableFuture<String> getAnnouncementOwner(
            final Integer announcementId) {
        String requestId = UUID.randomUUID().toString();
        AnnouncementOwnerRequest request =
                new AnnouncementOwnerRequest(requestId, announcementId);

        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        kafkaTemplate.send(REQUEST_TOPIC, request);

        // Optional: add timeout safeguard
        CompletableFuture.delayedExecutor(TIMEOUT_SECONDS,
                TimeUnit.SECONDS).execute(() -> {
                    if (!future.isDone()) {
                        future.complete(null);
                        pendingRequests.remove(requestId);
                    }
                });

        return future;
    }

    /**
     * Called by Kafka listener when a response arrives.
     *
     * @param response the owner response
     */
    public void handleResponse(
            final AnnouncementOwnerResponse response) {
        CompletableFuture<String> future =
                pendingRequests.remove(response.getRequestId());
        if (future != null) {
            future.complete(response.getOwnerUsername());
        }
    }
}
