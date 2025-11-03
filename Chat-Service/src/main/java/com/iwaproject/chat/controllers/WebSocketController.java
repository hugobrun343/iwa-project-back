package com.iwaproject.chat.controllers;

import com.iwaproject.chat.dto.MessageDTO;
import com.iwaproject.chat.repositories.DiscussionRepository;
import com.iwaproject.chat.services.ChatService;
import com.iwaproject.chat.services.KafkaLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket controller for real-time chat messaging.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    /**
     * Chat service.
     */
    private final ChatService chatService;

    /**
     * Discussion repository.
     */
    private final DiscussionRepository discussionRepository;

    /**
     * Kafka log service.
     */
    private final KafkaLogService kafkaLogService;

    /**
     * Logger name constant.
     */
    private static final String LOGGER_NAME = "WebSocketController";

    /**
     * Handle message sent via WebSocket.
     *
     * @param discussionId the discussion ID
     * @param messagePayload the message payload (contains "content", "authorId",
     *                      and optionally "announcementId" and "recipientId")
     * @param headerAccessor header accessor for session info
     * @return message DTO to broadcast
     */
    @MessageMapping("/discussions/{id}/messages")
    @SendTo("/topic/discussions/{id}/messages")
    public MessageDTO handleMessage(
            @DestinationVariable("id") final Long discussionId,
            @Payload final Map<String, Object> messagePayload,
            final SimpMessageHeaderAccessor headerAccessor) {

        String authorId = (String) messagePayload.get("authorId");
        String content = (String) messagePayload.get("content");
        Long announcementId = messagePayload.get("announcementId") != null
                ? ((Number) messagePayload.get("announcementId")).longValue()
                : null;
        String recipientId = (String) messagePayload.get("recipientId");

        log.info("WebSocket message received - Discussion: {}, Author: {}",
                discussionId, authorId);

        kafkaLogService.info(LOGGER_NAME,
                "WebSocket message - Discussion: " + discussionId
                + ", Author: " + authorId);

        try {
            // If discussionId is null or invalid, verify we have required info
            if (discussionId == null
                    && (announcementId == null || recipientId == null)) {
                log.warn("Missing discussionId or announcementId/recipientId");
                kafkaLogService.warn(LOGGER_NAME,
                        "Missing required fields for message creation");
                return null;
            }

            // Verify user is participant if discussion exists
            if (discussionId != null
                    && !discussionRepository.isParticipant(discussionId, authorId)) {
                log.warn("User {} is not a participant in discussion {}",
                        authorId, discussionId);
                kafkaLogService.warn(LOGGER_NAME,
                        "User not participant in discussion: " + discussionId);
                return null;
            }

            // Create message via service (this will also update/create discussion)
            MessageDTO messageDTO = chatService.createMessage(
                    discussionId, authorId, content, announcementId, recipientId);

            log.info("Message created via WebSocket: {}", messageDTO.getId());
            return messageDTO;

        } catch (Exception e) {
            log.error("Error handling WebSocket message: {}", e.getMessage(), e);
            kafkaLogService.error(LOGGER_NAME,
                    "Error handling WebSocket message: " + e.getMessage());
            return null;
        }
    }
}

