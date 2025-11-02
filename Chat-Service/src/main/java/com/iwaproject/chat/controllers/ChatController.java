package com.iwaproject.chat.controllers;

import com.iwaproject.chat.dto.CreateDiscussionDTO;
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
     * Create or get existing discussion.
     *
     * @param userId the user ID (from token, will be sender)
     * @param createDTO the creation DTO
     * @return discussion DTO
     */
    @PostMapping("/discussions")
    public ResponseEntity<DiscussionDTO> createDiscussion(
            @RequestHeader("X-Username") final String userId,
            @RequestBody final CreateDiscussionDTO createDTO) {

        kafkaLogService.info(LOGGER_NAME,
                "POST /discussions - User: " + userId
                + ", announcementId: " + createDTO.getAnnouncementId()
                + ", recipientId: " + createDTO.getRecipientId());

        try {
            // Validate required fields
            if (createDTO.getAnnouncementId() == null
                    || createDTO.getRecipientId() == null
                    || createDTO.getRecipientId().isBlank()) {
                kafkaLogService.warn(LOGGER_NAME,
                        "Missing required fields: announcementId or recipientId");
                return ResponseEntity.badRequest().build();
            }

            DiscussionDTO discussion = chatService.createOrGetDiscussion(
                    userId,
                    createDTO.getAnnouncementId(),
                    createDTO.getRecipientId());

            return ResponseEntity.created(
                            URI.create("/api/discussions/" + discussion.getId()))
                    .body(discussion);
        } catch (IllegalArgumentException e) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Invalid request: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to create discussion: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get discussion details by ID.
     *
     * @param id the discussion ID
     * @param userId the user ID (from token)
     * @return discussion DTO
     */
    @GetMapping("/discussions/{id}")
    public ResponseEntity<DiscussionDTO> getDiscussionById(
            @PathVariable("id") final Long id,
            @RequestHeader("X-Username") final String userId) {

        kafkaLogService.info(LOGGER_NAME,
                "GET /discussions/" + id + " - User: " + userId);

        try {
            DiscussionDTO discussion = chatService.getDiscussionById(id, userId);
            return ResponseEntity.ok(discussion);
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
     * Send a message in a discussion.
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
                    id, userId, createDTO.getContent());

            return ResponseEntity.created(
                            URI.create("/api/discussions/" + id
                                    + "/messages/" + message.getId()))
                    .body(message);
        } catch (IllegalArgumentException e) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Invalid request: " + e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to create message: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

