package com.iwaproject.chat.services;

import com.iwaproject.chat.dto.DiscussionDTO;
import com.iwaproject.chat.dto.MessageDTO;
import com.iwaproject.chat.entities.Discussion;
import com.iwaproject.chat.entities.Message;
import com.iwaproject.chat.repositories.DiscussionRepository;
import com.iwaproject.chat.repositories.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for chat operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    /**
     * Discussion repository.
     */
    private final DiscussionRepository discussionRepository;

    /**
     * Message repository.
     */
    private final MessageRepository messageRepository;

    /**
     * Kafka consumer service.
     */
    private final KafkaConsumerService kafkaConsumerService;

    /**
     * Kafka log service.
     */
    private final KafkaLogService kafkaLogService;

    /**
     * Logger name constant.
     */
    private static final String LOGGER_NAME = "ChatService";

    /**
     * Timeout for Kafka requests in seconds.
     */
    private static final int KAFKA_TIMEOUT_SECONDS = 5;

    /**
     * Get discussions for a user (where user is sender or recipient).
     *
     * @param userId the user ID
     * @param page page number (0-based)
     * @param limit page size
     * @return page of discussions
     */
    public Page<DiscussionDTO> getMyDiscussions(final String userId,
            final int page, final int limit) {
        log.debug("Fetching discussions for user: {} (page: {}, limit: {})",
                userId, page, limit);

        Pageable pageable = PageRequest.of(page, limit);
        Page<Discussion> discussions = discussionRepository
                .findBySenderIdOrRecipientId(userId, pageable);

        return discussions.map(this::mapToDiscussionDTO);
    }

    /**
     * Create or get existing discussion.
     * Creates discussion if it doesn't exist, returns existing one otherwise.
     *
     * @param senderId the sender ID (current user)
     * @param announcementId the announcement ID
     * @param recipientId the recipient ID
     * @return discussion DTO
     */
    @Transactional
    public DiscussionDTO createOrGetDiscussion(final String senderId,
            final Long announcementId, final String recipientId) {
        log.info("Creating or getting discussion for sender: {}, "
                + "recipient: {}, announcement: {}",
                senderId, recipientId, announcementId);

        // Verify recipient exists via Kafka
        if (!verifyUserExists(recipientId)) {
            throw new IllegalArgumentException(
                    "Recipient user does not exist: " + recipientId);
        }

        // Check if discussion already exists
        Optional<Discussion> existing = discussionRepository
                .findByAnnouncementIdAndParticipants(announcementId,
                        senderId, recipientId);

        if (existing.isPresent()) {
            log.debug("Discussion already exists: {}", existing.get().getId());
            return mapToDiscussionDTO(existing.get());
        }

        // Create new discussion
        Discussion discussion = new Discussion();
        discussion.setAnnouncementId(announcementId);
        discussion.setSenderId(senderId);
        discussion.setRecipientId(recipientId);
        discussion.setCreatedAt(LocalDateTime.now());
        discussion.setUpdatedAt(LocalDateTime.now());

        Discussion saved = discussionRepository.save(discussion);
        log.info("Created new discussion: {}", saved.getId());

        return mapToDiscussionDTO(saved);
    }

    /**
     * Get discussion by ID (if user is participant).
     *
     * @param discussionId the discussion ID
     * @param userId the user ID
     * @return discussion DTO
     */
    public DiscussionDTO getDiscussionById(final Long discussionId,
            final String userId) {
        log.debug("Fetching discussion: {} for user: {}", discussionId, userId);

        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Discussion not found: " + discussionId));

        // Verify user is participant
        if (!discussion.getSenderId().equals(userId)
                && !discussion.getRecipientId().equals(userId)) {
            throw new IllegalArgumentException(
                    "User is not a participant in this discussion");
        }

        return mapToDiscussionDTO(discussion);
    }

    /**
     * Get messages for a discussion.
     *
     * @param discussionId the discussion ID
     * @param userId the user ID (for verification)
     * @param page page number (0-based)
     * @param limit page size
     * @return page of messages
     */
    public Page<MessageDTO> getMessagesByDiscussionId(
            final Long discussionId, final String userId,
            final int page, final int limit) {
        log.debug("Fetching messages for discussion: {} (page: {}, limit: {})",
                discussionId, page, limit);

        // Verify user is participant
        if (!discussionRepository.isParticipant(discussionId, userId)) {
            throw new IllegalArgumentException(
                    "User is not a participant in this discussion");
        }

        Pageable pageable = PageRequest.of(page, limit);
        Page<Message> messages = messageRepository
                .findByDiscussionId(discussionId, pageable);

        return messages.map(this::mapToMessageDTO);
    }

    /**
     * Create a new message in a discussion.
     *
     * @param discussionId the discussion ID
     * @param authorId the author ID (must match userId from token)
     * @param content the message content
     * @return message DTO
     */
    @Transactional
    public MessageDTO createMessage(final Long discussionId,
            final String authorId, final String content) {
        log.info("Creating message in discussion: {} by author: {}",
                discussionId, authorId);

        // Verify user is participant
        if (!discussionRepository.isParticipant(discussionId, authorId)) {
            throw new IllegalArgumentException(
                    "User is not a participant in this discussion");
        }

        // Get discussion
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Discussion not found: " + discussionId));

        // Create message
        Message message = new Message();
        message.setDiscussion(discussion);
        message.setAuthorId(authorId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);

        // Update discussion updatedAt
        discussion.setUpdatedAt(LocalDateTime.now());
        discussionRepository.save(discussion);

        log.info("Created message: {} in discussion: {}", saved.getId(),
                discussionId);

        return mapToMessageDTO(saved);
    }

    /**
     * Verify if a user exists via Kafka.
     *
     * @param userId the user ID to verify
     * @return true if user exists, false otherwise
     */
    private boolean verifyUserExists(final String userId) {
        try {
            CompletableFuture<String> future = kafkaConsumerService
                    .checkUserExists(userId);
            String result = future.get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return "true".equals(result);
        } catch (Exception e) {
            log.error("Error verifying user existence for: {}", userId, e);
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to verify user existence for: " + userId);
            return false;
        }
    }

    /**
     * Map Discussion entity to DiscussionDTO.
     *
     * @param discussion the discussion entity
     * @return discussion DTO
     */
    private DiscussionDTO mapToDiscussionDTO(final Discussion discussion) {
        DiscussionDTO dto = new DiscussionDTO();
        dto.setId(discussion.getId());
        dto.setAnnouncementId(discussion.getAnnouncementId());
        dto.setSenderId(discussion.getSenderId());
        dto.setRecipientId(discussion.getRecipientId());
        dto.setCreatedAt(discussion.getCreatedAt());
        dto.setUpdatedAt(discussion.getUpdatedAt());
        return dto;
    }

    /**
     * Map Message entity to MessageDTO.
     *
     * @param message the message entity
     * @return message DTO
     */
    private MessageDTO mapToMessageDTO(final Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setDiscussionId(message.getDiscussion().getId());
        dto.setAuthorId(message.getAuthorId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}

