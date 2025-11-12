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
     * Returns empty discussion if not found.
     *
     * @param discussionId the discussion ID
     * @param userId the user ID
     * @return discussion DTO (empty if not found)
     */
    public DiscussionDTO getDiscussionById(final Long discussionId,
            final String userId) {
        log.debug("Fetching discussion: {} for user: {}", discussionId, userId);

        Optional<Discussion> discussionOpt = discussionRepository
                .findById(discussionId);

        if (discussionOpt.isEmpty()) {
            log.debug("Discussion not found: {}, returning empty discussion",
                    discussionId);
            return createEmptyDiscussionDTO();
        }

        Discussion discussion = discussionOpt.get();

        // Verify user is participant
        if (!discussion.getSenderId().equals(userId)
                && !discussion.getRecipientId().equals(userId)) {
            log.debug("User {} is not a participant in discussion {}",
                    userId, discussionId);
            return createEmptyDiscussionDTO();
        }

        return mapToDiscussionDTO(discussion);
    }

    /**
     * Get or create discussion by announcement ID and participants.
     * Returns empty discussion if not found (doesn't create it yet).
     *
     * @param announcementId the announcement ID
     * @param userId the user ID (current user)
     * @param recipientId the recipient ID
     * @return discussion DTO (empty if not found)
     */
    public DiscussionDTO getDiscussionByAnnouncementAndParticipants(
            final Long announcementId, final String userId,
            final String recipientId) {
        log.debug("Fetching discussion for announcement: {}, user: {}, "
                + "recipient: {}", announcementId, userId, recipientId);

        Optional<Discussion> existing = discussionRepository
                .findByAnnouncementIdAndParticipants(announcementId,
                        userId, recipientId);

        if (existing.isPresent()) {
            Discussion discussion = existing.get();
            // Verify user is participant
            if (!discussion.getSenderId().equals(userId)
                    && !discussion.getRecipientId().equals(userId)) {
                log.debug("User {} is not a participant", userId);
                return createEmptyDiscussionDTO();
            }
            return mapToDiscussionDTO(discussion);
        }

        log.debug("Discussion not found, returning empty discussion");
        return createEmptyDiscussionDTO();
    }

    /**
     * Create an empty discussion DTO.
     *
     * @return empty discussion DTO
     */
    private DiscussionDTO createEmptyDiscussionDTO() {
        DiscussionDTO dto = new DiscussionDTO();
        dto.setId(null);
        dto.setAnnouncementId(null);
        dto.setSenderId(null);
        dto.setRecipientId(null);
        dto.setCreatedAt(null);
        dto.setUpdatedAt(null);
        return dto;
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
     * Creates the discussion automatically if it doesn't exist
     * (requires announcementId and recipientId in the request).
     *
     * @param discussionId the discussion ID (can be null if creating new discussion)
     * @param authorId the author ID (must match userId from token)
     * @param content the message content
     * @param announcementId the announcement ID (required if discussionId is null)
     * @param recipientId the recipient ID (required if discussionId is null)
     * @return message DTO
     */
    @Transactional
    public MessageDTO createMessage(final Long discussionId,
            final String authorId, final String content,
            final Long announcementId, final String recipientId) {
        log.info("Creating message in discussion: {} by author: {}",
                discussionId, authorId);

        Discussion discussion;

        // If discussionId is provided, try to get existing discussion
        if (discussionId != null) {
            Optional<Discussion> discussionOpt = discussionRepository
                    .findById(discussionId);

            if (discussionOpt.isPresent()) {
                discussion = discussionOpt.get();
                // Verify user is participant
                if (!discussion.getSenderId().equals(authorId)
                        && !discussion.getRecipientId().equals(authorId)) {
                    throw new IllegalArgumentException(
                            "User is not a participant in this discussion");
                }
            } else {
                // Discussion doesn't exist, need to create it
                if (announcementId == null || recipientId == null
                        || recipientId.isBlank()) {
                    throw new IllegalArgumentException(
                            "Discussion not found and missing announcementId "
                            + "or recipientId to create it");
                }
                discussion = createOrGetDiscussionInternal(authorId,
                        announcementId, recipientId);
            }
        } else {
            // No discussionId provided, create new discussion
            if (announcementId == null || recipientId == null
                    || recipientId.isBlank()) {
                throw new IllegalArgumentException(
                        "Missing announcementId or recipientId to create "
                        + "discussion");
            }
            discussion = createOrGetDiscussionInternal(authorId,
                    announcementId, recipientId);
        }

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
                discussion.getId());

        return mapToMessageDTO(saved);
    }

    /**
     * Create or get existing discussion (internal method without Kafka verification).
     *
     * @param senderId the sender ID (current user)
     * @param announcementId the announcement ID
     * @param recipientId the recipient ID
     * @return discussion entity
     */
    private Discussion createOrGetDiscussionInternal(final String senderId,
            final Long announcementId, final String recipientId) {
        log.info("Creating or getting discussion for sender: {}, "
                + "recipient: {}, announcement: {}",
                senderId, recipientId, announcementId);

        // Check if discussion already exists
        Optional<Discussion> existing = discussionRepository
                .findByAnnouncementIdAndParticipants(announcementId,
                        senderId, recipientId);

        if (existing.isPresent()) {
            log.debug("Discussion already exists: {}", existing.get().getId());
            return existing.get();
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

        return saved;
    }

    /**
     * Delete a discussion by ID.
     *
     * @param discussionId the discussion ID
     * @param userId the user ID (must be participant)
     */
    @Transactional
    public void deleteDiscussion(final Long discussionId,
            final String userId) {
        log.info("Deleting discussion: {} by user: {}", discussionId, userId);

        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Discussion not found: " + discussionId));

        // Verify user is participant
        if (!discussion.getSenderId().equals(userId)
                && !discussion.getRecipientId().equals(userId)) {
            throw new IllegalArgumentException(
                    "User is not a participant in this discussion");
        }

        // Delete all messages first (cascade might not be configured)
        messageRepository.deleteByDiscussionId(discussionId);

        // Delete discussion
        discussionRepository.delete(discussion);

        log.info("Deleted discussion: {}", discussionId);
    }

    /**
     * Verify if a user exists via Kafka.
     * Returns true on timeout to allow degraded mode operation.
     *
     * @param userId the user ID to verify
     * @return true if user exists, false otherwise
     */
    private boolean verifyUserExists(final String userId) {
        try {
            CompletableFuture<String> future = kafkaConsumerService
                    .checkUserExists(userId)
                    .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String result = future.get();
            return "true".equals(result);
        } catch (Exception e) {
            // Check if it's a timeout exception (wrapped or direct)
            if (e instanceof java.util.concurrent.TimeoutException 
                    || (e.getCause() instanceof java.util.concurrent.TimeoutException)) {
                // Accept by default on timeout (degraded mode)
                log.warn("Kafka timeout verifying user {}, accepting by default", userId);
                kafkaLogService.warn(LOGGER_NAME,
                        "Kafka timeout for user verification: " + userId + " - accepting");
                return true;
            }
            // Reject on other errors (network, format, etc.)
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

