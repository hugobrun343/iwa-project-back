package com.iwaproject.chat.controllers;

import com.iwaproject.chat.dto.CreateMessageDTO;
import com.iwaproject.chat.dto.DiscussionDTO;
import com.iwaproject.chat.dto.MessageDTO;
import com.iwaproject.chat.services.ChatService;
import com.iwaproject.chat.services.KafkaLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Main controller for chat operations.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    /**
     * Chat service.
     */
    private final ChatService chatService;

    /**
     * Kafka log service.
     */
    private final KafkaLogService kafkaLogService;

    /**
     * Logger name constant.
     */
    private static final String LOGGER_NAME = "ChatController";


    /**
     * Get my discussions (where user is sender or recipient).
     *
     * @param userId the user ID (from token)
     * @param page page number (optional, default: 0)
     * @param limit page size (optional, default: 20)
     * @return page of discussions
     */
    @GetMapping("/me/discussions")
    public ResponseEntity<Page<DiscussionDTO>> getMyDiscussions(
            @RequestHeader("X-Username") final String userId,
            @RequestParam(value = "page", required = false,
                    defaultValue = "0") final int page,
            @RequestParam(value = "limit", required = false,
                    defaultValue = "20") final int limit) {

        // Validate X-Username header
        if (userId == null || userId.trim().isEmpty()) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Missing or empty X-Username header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        kafkaLogService.info(LOGGER_NAME,
                "GET /me/discussions - User: " + userId
                + ", page: " + page + ", limit: " + limit);

        try {
            Page<DiscussionDTO> discussions = chatService.getMyDiscussions(
                    userId, page, limit);
            return ResponseEntity.ok(discussions);
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to get discussions: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get discussion by announcement ID and participants.
     * Returns empty discussion if not found.
     *
     * @param userId the user ID (from token)
     * @param announcementId the announcement ID
     * @param recipientId the recipient ID
     * @return discussion DTO (empty if not found)
     */
    @GetMapping("/discussions")
    public ResponseEntity<DiscussionDTO> getDiscussionByAnnouncement(
            @RequestHeader("X-Username") final String userId,
            @RequestParam("announcementId") final Long announcementId,
            @RequestParam("recipientId") final String recipientId) {

        // Validate X-Username header
        if (userId == null || userId.trim().isEmpty()) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Missing or empty X-Username header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        kafkaLogService.info(LOGGER_NAME,
                "GET /discussions - User: " + userId
                + ", announcementId: " + announcementId
                + ", recipientId: " + recipientId);

        try {
            DiscussionDTO discussion =
                    chatService.getDiscussionByAnnouncementAndParticipants(
                            announcementId, userId, recipientId);
            return ResponseEntity.ok(discussion);
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to get discussion: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get discussion details by ID.
     * Returns empty discussion if not found or user is not participant.
     *
     * @param id the discussion ID
     * @param userId the user ID (from token)
     * @return discussion DTO (empty if not found)
     */
    @GetMapping("/discussions/{id}")
    public ResponseEntity<DiscussionDTO> getDiscussionById(
            @PathVariable("id") final Long id,
            @RequestHeader("X-Username") final String userId) {

        // Validate X-Username header
        if (userId == null || userId.trim().isEmpty()) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Missing or empty X-Username header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        kafkaLogService.info(LOGGER_NAME,
                "GET /discussions/" + id + " - User: " + userId);

        try {
            DiscussionDTO discussion = chatService.getDiscussionById(id, userId);
            return ResponseEntity.ok(discussion);
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to get discussion: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get messages for a discussion.
     *
     * @param id the discussion ID
     * @param userId the user ID (from token)
     * @param page page number (optional, default: 0)
     * @param limit page size (optional, default: 20)
     * @return page of messages
     */
    @GetMapping("/discussions/{id}/messages")
    public ResponseEntity<Page<MessageDTO>> getMessagesByDiscussionId(
            @PathVariable("id") final Long id,
            @RequestHeader("X-Username") final String userId,
            @RequestParam(value = "page", required = false,
                    defaultValue = "0") final int page,
            @RequestParam(value = "limit", required = false,
                    defaultValue = "20") final int limit) {

        // Validate X-Username header
        if (userId == null || userId.trim().isEmpty()) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Missing or empty X-Username header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        kafkaLogService.info(LOGGER_NAME,
                "GET /discussions/" + id + "/messages - User: " + userId
                + ", page: " + page + ", limit: " + limit);

        try {
            Page<MessageDTO> messages = chatService.getMessagesByDiscussionId(
                    id, userId, page, limit);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Discussion not found or user not participant: "
                    + e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to get messages: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Send a message without specifying a discussion ID.
     * Creates the discussion automatically based on announcementId and recipientId.
     * This endpoint is useful for creating the first message in a new discussion.
     *
     * @param userId the user ID (from token, must match author)
     * @param createDTO the message creation DTO (must include announcementId and recipientId)
     * @return message DTO
     */
    @PostMapping("/messages")
    public ResponseEntity<MessageDTO> createMessageWithoutDiscussion(
            @RequestHeader("X-Username") final String userId,
            @RequestBody final CreateMessageDTO createDTO) {

        // Validate X-Username header
        if (userId == null || userId.trim().isEmpty()) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Missing or empty X-Username header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        kafkaLogService.info(LOGGER_NAME,
                "POST /messages - User: " + userId);

        try {
            // Validate required fields
            if (createDTO.getContent() == null
                    || createDTO.getContent().isBlank()) {
                kafkaLogService.warn(LOGGER_NAME,
                        "Missing required field: content");
                return ResponseEntity.badRequest().build();
            }

            if (createDTO.getAnnouncementId() == null
                    || createDTO.getRecipientId() == null
                    || createDTO.getRecipientId().isBlank()) {
                kafkaLogService.warn(LOGGER_NAME,
                        "Missing required fields: announcementId or recipientId");
                return ResponseEntity.badRequest().build();
            }

            MessageDTO message = chatService.createMessage(
                    null, userId, createDTO.getContent(),
                    createDTO.getAnnouncementId(),
                    createDTO.getRecipientId());

            return ResponseEntity.created(
                            URI.create("/api/discussions/"
                                    + message.getDiscussionId()
                                    + "/messages/" + message.getId()))
                    .body(message);
        } catch (IllegalArgumentException e) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Invalid request: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to create message: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Send a message in a discussion.
     * Creates the discussion automatically if it doesn't exist
     * (requires announcementId and recipientId in the request body).
     *
     * @param id the discussion ID
     * @param userId the user ID (from token, must match author)
     * @param createDTO the message creation DTO
     * @return message DTO
     */
    @PostMapping("/discussions/{id}/messages")
    public ResponseEntity<MessageDTO> createMessage(
            @PathVariable("id") final Long id,
            @RequestHeader("X-Username") final String userId,
            @RequestBody final CreateMessageDTO createDTO) {

        // Validate X-Username header
        if (userId == null || userId.trim().isEmpty()) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Missing or empty X-Username header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        kafkaLogService.info(LOGGER_NAME,
                "POST /discussions/" + id + "/messages - User: " + userId);

        try {
            // Validate required fields
            if (createDTO.getContent() == null
                    || createDTO.getContent().isBlank()) {
                kafkaLogService.warn(LOGGER_NAME,
                        "Missing required field: content");
                return ResponseEntity.badRequest().build();
            }

            MessageDTO message = chatService.createMessage(
                    id, userId, createDTO.getContent(),
                    createDTO.getAnnouncementId(),
                    createDTO.getRecipientId());

            return ResponseEntity.created(
                            URI.create("/api/discussions/"
                                    + message.getDiscussionId()
                                    + "/messages/" + message.getId()))
                    .body(message);
        } catch (IllegalArgumentException e) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Invalid request: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to create message: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a discussion by ID.
     *
     * @param id the discussion ID
     * @param userId the user ID (from token, must be participant)
     * @return no content if successful
     */
    @DeleteMapping("/discussions/{id}")
    public ResponseEntity<Void> deleteDiscussion(
            @PathVariable("id") final Long id,
            @RequestHeader("X-Username") final String userId) {

        // Validate X-Username header
        if (userId == null || userId.trim().isEmpty()) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Missing or empty X-Username header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        kafkaLogService.info(LOGGER_NAME,
                "DELETE /discussions/" + id + " - User: " + userId);

        try {
            chatService.deleteDiscussion(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Invalid request: " + e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to delete discussion: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

