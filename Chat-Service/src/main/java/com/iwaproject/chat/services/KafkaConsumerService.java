package com.iwaproject.chat.services;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Kafka consumer service for chat-related messages.
 */
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    /**
     * Logger name constant.
     */
    private static final String LOGGER_NAME = "KafkaConsumerService";

    /**
     * Kafka producer service.
     */
    private final KafkaProducerService kafkaProducerService;

    /**
     * Kafka log service.
     */
    private final KafkaLogService kafkaLogService;

    /**
     * Pending requests waiting for responses.
     */
    private final Map<String, CompletableFuture<String>> pendingRequests =
            new ConcurrentHashMap<>();

    /**
     * Consume user existence check responses from User-Service.
     * Message format: <correlationId>:true|false
     *
     * @param message the kafka message
     */
    @KafkaListener(topics = "chat-user-exists-reply",
            groupId = "chat-user-exists-reply-group")
    public void consumeUserExistsReply(final String message) {
        kafkaLogService.info(LOGGER_NAME,
                "Received message on 'chat-user-exists-reply': " + message);

        String[] parts = message.split(":", 2);
        if (parts.length < 2) {
            kafkaLogService.error(LOGGER_NAME,
                    "Invalid message format. Expected format: "
                    + "<correlationId>:true|false");
            return;
        }

        String correlationId = parts[0];
        String result = parts[1];

        CompletableFuture<String> future = pendingRequests.remove(correlationId);
        if (future != null) {
            future.complete(result);
        } else {
            kafkaLogService.warn(LOGGER_NAME,
                    "No pending request found for correlationId: "
                    + correlationId);
        }
    }

    /**
     * Send user existence check request to User-Service.
     * Message format: <correlationId>:<replyTopic>:<username>
     *
     * @param username the username to check
     * @return CompletableFuture with the result (true or false)
     */
    public CompletableFuture<String> checkUserExists(final String username) {
        String correlationId = UUID.randomUUID().toString();
        String replyTopic = "chat-user-exists-reply";
        String request = correlationId + ":" + replyTopic + ":" + username;

        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        kafkaProducerService.sendMessage("user-exists-topic", request);
        kafkaLogService.info(LOGGER_NAME,
                "Sent user exists request to User-Service for username: "
                + username);

        return future;
    }
}

