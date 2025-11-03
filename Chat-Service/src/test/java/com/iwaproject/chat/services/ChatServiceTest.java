package com.iwaproject.chat.services;

import com.iwaproject.chat.dto.DiscussionDTO;
import com.iwaproject.chat.dto.MessageDTO;
import com.iwaproject.chat.entities.Discussion;
import com.iwaproject.chat.entities.Message;
import com.iwaproject.chat.repositories.DiscussionRepository;
import com.iwaproject.chat.repositories.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for ChatService.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    /**
     * Mock repositories.
     */
    @Mock
    private DiscussionRepository discussionRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private KafkaConsumerService kafkaConsumerService;
    @Mock
    private KafkaLogService kafkaLogService;

    /**
     * Service under test.
     */
    @InjectMocks
    private ChatService chatService;

    /**
     * Test constants.
     */
    private static final String TEST_SENDER_ID = "sender-123";
    private static final String TEST_RECIPIENT_ID = "recipient-456";
    private static final Long TEST_ANNOUNCEMENT_ID = 1L;
    private static final Long TEST_DISCUSSION_ID = 100L;
    private static final Long TEST_MESSAGE_ID = 200L;

    /**
     * Test discussion.
     */
    private Discussion testDiscussion;

    /**
     * Test message.
     */
    private Message testMessage;

    /**
     * Setup test data.
     */
    @BeforeEach
    void setUp() {
        testDiscussion = createTestDiscussion();
        testMessage = createTestMessage();
    }

    /**
     * Test getMyDiscussions returns paginated discussions.
     */
    @Test
    @DisplayName("getMyDiscussions should return paginated discussions")
    void getMyDiscussions_shouldReturnPaginatedDiscussions() {
        // Given
        int page = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(page, limit);
        Page<Discussion> discussionPage = new PageImpl<>(
                List.of(testDiscussion), pageable, 1);

        when(discussionRepository.findBySenderIdOrRecipientId(
                TEST_SENDER_ID, pageable))
                .thenReturn(discussionPage);

        // When
        Page<DiscussionDTO> result = chatService.getMyDiscussions(
                TEST_SENDER_ID, page, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(TEST_DISCUSSION_ID, result.getContent().get(0).getId());
        verify(discussionRepository).findBySenderIdOrRecipientId(
                TEST_SENDER_ID, pageable);
    }

    /**
     * Test createOrGetDiscussion when discussion does not exist.
     */
    @Test
    @DisplayName("createOrGetDiscussion should create new discussion when not exists")
    void createOrGetDiscussion_shouldCreateNewDiscussion() {
        // Given
        CompletableFuture<String> future = CompletableFuture
                .completedFuture("true");
        when(kafkaConsumerService.checkUserExists(TEST_RECIPIENT_ID))
                .thenReturn(future);
        when(discussionRepository.findByAnnouncementIdAndParticipants(
                TEST_ANNOUNCEMENT_ID, TEST_SENDER_ID, TEST_RECIPIENT_ID))
                .thenReturn(Optional.empty());
        when(discussionRepository.save(any(Discussion.class)))
                .thenReturn(testDiscussion);

        // When
        DiscussionDTO result = chatService.createOrGetDiscussion(
                TEST_SENDER_ID, TEST_ANNOUNCEMENT_ID, TEST_RECIPIENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DISCUSSION_ID, result.getId());
        verify(discussionRepository).save(any(Discussion.class));
    }

    /**
     * Test createOrGetDiscussion when discussion already exists.
     */
    @Test
    @DisplayName("createOrGetDiscussion should return existing discussion")
    void createOrGetDiscussion_shouldReturnExistingDiscussion() {
        // Given
        CompletableFuture<String> future = CompletableFuture
                .completedFuture("true");
        when(kafkaConsumerService.checkUserExists(TEST_RECIPIENT_ID))
                .thenReturn(future);
        when(discussionRepository.findByAnnouncementIdAndParticipants(
                TEST_ANNOUNCEMENT_ID, TEST_SENDER_ID, TEST_RECIPIENT_ID))
                .thenReturn(Optional.of(testDiscussion));

        // When
        DiscussionDTO result = chatService.createOrGetDiscussion(
                TEST_SENDER_ID, TEST_ANNOUNCEMENT_ID, TEST_RECIPIENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DISCUSSION_ID, result.getId());
        verify(discussionRepository, never()).save(any(Discussion.class));
    }

    /**
     * Test createOrGetDiscussion when recipient does not exist.
     */
    @Test
    @DisplayName("createOrGetDiscussion should throw when recipient does not exist")
    void createOrGetDiscussion_shouldThrowWhenRecipientNotFound() {
        // Given
        CompletableFuture<String> future = CompletableFuture
                .completedFuture("false");
        when(kafkaConsumerService.checkUserExists(TEST_RECIPIENT_ID))
                .thenReturn(future);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            chatService.createOrGetDiscussion(
                    TEST_SENDER_ID, TEST_ANNOUNCEMENT_ID, TEST_RECIPIENT_ID);
        });
        verify(discussionRepository, never()).save(any(Discussion.class));
    }

    /**
     * Test getDiscussionById when user is participant.
     */
    @Test
    @DisplayName("getDiscussionById should return discussion when user is participant")
    void getDiscussionById_shouldReturnDiscussionWhenParticipant() {
        // Given
        when(discussionRepository.findById(TEST_DISCUSSION_ID))
                .thenReturn(Optional.of(testDiscussion));

        // When
        DiscussionDTO result = chatService.getDiscussionById(
                TEST_DISCUSSION_ID, TEST_SENDER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DISCUSSION_ID, result.getId());
        verify(discussionRepository).findById(TEST_DISCUSSION_ID);
    }

    /**
     * Test getDiscussionById when user is not participant returns empty discussion.
     */
    @Test
    @DisplayName("getDiscussionById should return empty discussion when user is not participant")
    void getDiscussionById_shouldReturnEmptyWhenNotParticipant() {
        // Given
        String otherUserId = "other-user";
        when(discussionRepository.findById(TEST_DISCUSSION_ID))
                .thenReturn(Optional.of(testDiscussion));

        // When
        DiscussionDTO result = chatService.getDiscussionById(
                TEST_DISCUSSION_ID, otherUserId);

        // Then
        assertNotNull(result);
        assertEquals(null, result.getId());
        assertEquals(null, result.getSenderId());
    }

    /**
     * Test getDiscussionById when discussion does not exist returns empty discussion.
     */
    @Test
    @DisplayName("getDiscussionById should return empty discussion when not found")
    void getDiscussionById_shouldReturnEmptyWhenNotFound() {
        // Given
        when(discussionRepository.findById(TEST_DISCUSSION_ID))
                .thenReturn(Optional.empty());

        // When
        DiscussionDTO result = chatService.getDiscussionById(
                TEST_DISCUSSION_ID, TEST_SENDER_ID);

        // Then
        assertNotNull(result);
        assertEquals(null, result.getId());
        assertEquals(null, result.getSenderId());
    }

    /**
     * Test getDiscussionByAnnouncementAndParticipants returns discussion when exists.
     */
    @Test
    @DisplayName("getDiscussionByAnnouncementAndParticipants should return discussion when exists")
    void getDiscussionByAnnouncementAndParticipants_shouldReturnDiscussion() {
        // Given
        when(discussionRepository.findByAnnouncementIdAndParticipants(
                TEST_ANNOUNCEMENT_ID, TEST_SENDER_ID, TEST_RECIPIENT_ID))
                .thenReturn(Optional.of(testDiscussion));

        // When
        DiscussionDTO result = chatService.getDiscussionByAnnouncementAndParticipants(
                TEST_ANNOUNCEMENT_ID, TEST_SENDER_ID, TEST_RECIPIENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DISCUSSION_ID, result.getId());
        verify(discussionRepository).findByAnnouncementIdAndParticipants(
                TEST_ANNOUNCEMENT_ID, TEST_SENDER_ID, TEST_RECIPIENT_ID);
    }

    /**
     * Test getDiscussionByAnnouncementAndParticipants returns empty when not found.
     */
    @Test
    @DisplayName("getDiscussionByAnnouncementAndParticipants should return empty when not found")
    void getDiscussionByAnnouncementAndParticipants_shouldReturnEmptyWhenNotFound() {
        // Given
        when(discussionRepository.findByAnnouncementIdAndParticipants(
                TEST_ANNOUNCEMENT_ID, TEST_SENDER_ID, TEST_RECIPIENT_ID))
                .thenReturn(Optional.empty());

        // When
        DiscussionDTO result = chatService.getDiscussionByAnnouncementAndParticipants(
                TEST_ANNOUNCEMENT_ID, TEST_SENDER_ID, TEST_RECIPIENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(null, result.getId());
        assertEquals(null, result.getSenderId());
    }

    /**
     * Test getMessagesByDiscussionId returns paginated messages.
     */
    @Test
    @DisplayName("getMessagesByDiscussionId should return paginated messages")
    void getMessagesByDiscussionId_shouldReturnPaginatedMessages() {
        // Given
        int page = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(page, limit);
        Page<Message> messagePage = new PageImpl<>(
                List.of(testMessage), pageable, 1);

        when(discussionRepository.isParticipant(
                TEST_DISCUSSION_ID, TEST_SENDER_ID))
                .thenReturn(true);
        when(messageRepository.findByDiscussionId(
                TEST_DISCUSSION_ID, pageable))
                .thenReturn(messagePage);

        // When
        Page<MessageDTO> result = chatService.getMessagesByDiscussionId(
                TEST_DISCUSSION_ID, TEST_SENDER_ID, page, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(TEST_MESSAGE_ID, result.getContent().get(0).getId());
        verify(discussionRepository).isParticipant(
                TEST_DISCUSSION_ID, TEST_SENDER_ID);
    }

    /**
     * Test getMessagesByDiscussionId when user is not participant.
     */
    @Test
    @DisplayName("getMessagesByDiscussionId should throw when user is not participant")
    void getMessagesByDiscussionId_shouldThrowWhenNotParticipant() {
        // Given
        when(discussionRepository.isParticipant(
                TEST_DISCUSSION_ID, TEST_SENDER_ID))
                .thenReturn(false);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            chatService.getMessagesByDiscussionId(
                    TEST_DISCUSSION_ID, TEST_SENDER_ID, 0, 10);
        });
        verify(messageRepository, never()).findByDiscussionId(
                any(), any(Pageable.class));
    }

    /**
     * Test createMessage creates new message.
     */
    @Test
    @DisplayName("createMessage should create new message")
    void createMessage_shouldCreateNewMessage() {
        // Given
        String content = "Hello, test message";
        when(discussionRepository.findById(TEST_DISCUSSION_ID))
                .thenReturn(Optional.of(testDiscussion));
        when(messageRepository.save(any(Message.class)))
                .thenReturn(testMessage);
        when(discussionRepository.save(any(Discussion.class)))
                .thenReturn(testDiscussion);

        // When
        MessageDTO result = chatService.createMessage(
                TEST_DISCUSSION_ID, TEST_SENDER_ID, content, null, null);

        // Then
        assertNotNull(result);
        assertEquals(TEST_MESSAGE_ID, result.getId());
        verify(messageRepository).save(any(Message.class));
        verify(discussionRepository).save(any(Discussion.class));
    }

    /**
     * Test createMessage creates discussion automatically when it doesn't exist.
     */
    @Test
    @DisplayName("createMessage should create discussion automatically when it doesn't exist")
    void createMessage_shouldCreateDiscussionAutomatically() {
        // Given
        String content = "Hello, test message";
        Long nonExistentDiscussionId = 999L;
        when(discussionRepository.findById(nonExistentDiscussionId))
                .thenReturn(Optional.empty());
        when(discussionRepository.findByAnnouncementIdAndParticipants(
                TEST_ANNOUNCEMENT_ID, TEST_SENDER_ID, TEST_RECIPIENT_ID))
                .thenReturn(Optional.empty());
        when(discussionRepository.save(any(Discussion.class)))
                .thenReturn(testDiscussion);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(testMessage);

        // When
        MessageDTO result = chatService.createMessage(
                nonExistentDiscussionId, TEST_SENDER_ID, content,
                TEST_ANNOUNCEMENT_ID, TEST_RECIPIENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_MESSAGE_ID, result.getId());
        // Discussion is saved twice: once when created, once when updatedAt is set
        verify(discussionRepository, times(2)).save(any(Discussion.class));
        verify(messageRepository).save(any(Message.class));
    }

    /**
     * Test createMessage when user is not participant.
     */
    @Test
    @DisplayName("createMessage should throw when user is not participant")
    void createMessage_shouldThrowWhenNotParticipant() {
        // Given
        String content = "Hello";
        Discussion otherDiscussion = createTestDiscussion();
        otherDiscussion.setSenderId("other-sender");
        otherDiscussion.setRecipientId("other-recipient");
        when(discussionRepository.findById(TEST_DISCUSSION_ID))
                .thenReturn(Optional.of(otherDiscussion));

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            chatService.createMessage(
                    TEST_DISCUSSION_ID, TEST_SENDER_ID, content, null, null);
        });
        verify(messageRepository, never()).save(any(Message.class));
    }

    /**
     * Test deleteDiscussion deletes discussion successfully.
     */
    @Test
    @DisplayName("deleteDiscussion should delete discussion successfully")
    void deleteDiscussion_shouldDeleteSuccessfully() {
        // Given
        when(discussionRepository.findById(TEST_DISCUSSION_ID))
                .thenReturn(Optional.of(testDiscussion));
        willDoNothing().given(messageRepository)
                .deleteByDiscussionId(TEST_DISCUSSION_ID);
        willDoNothing().given(discussionRepository)
                .delete(testDiscussion);

        // When
        chatService.deleteDiscussion(TEST_DISCUSSION_ID, TEST_SENDER_ID);

        // Then
        verify(messageRepository).deleteByDiscussionId(TEST_DISCUSSION_ID);
        verify(discussionRepository).delete(testDiscussion);
    }

    /**
     * Test deleteDiscussion throws when discussion not found.
     */
    @Test
    @DisplayName("deleteDiscussion should throw when discussion not found")
    void deleteDiscussion_shouldThrowWhenNotFound() {
        // Given
        when(discussionRepository.findById(TEST_DISCUSSION_ID))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            chatService.deleteDiscussion(TEST_DISCUSSION_ID, TEST_SENDER_ID);
        });
        verify(messageRepository, never()).deleteByDiscussionId(any());
        verify(discussionRepository, never()).delete(any());
    }

    /**
     * Test deleteDiscussion throws when user is not participant.
     */
    @Test
    @DisplayName("deleteDiscussion should throw when user is not participant")
    void deleteDiscussion_shouldThrowWhenNotParticipant() {
        // Given
        String otherUserId = "other-user";
        when(discussionRepository.findById(TEST_DISCUSSION_ID))
                .thenReturn(Optional.of(testDiscussion));

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            chatService.deleteDiscussion(TEST_DISCUSSION_ID, otherUserId);
        });
        verify(messageRepository, never()).deleteByDiscussionId(any());
        verify(discussionRepository, never()).delete(any());
    }

    /**
     * Create test discussion.
     *
     * @return test discussion
     */
    private Discussion createTestDiscussion() {
        Discussion discussion = new Discussion();
        discussion.setId(TEST_DISCUSSION_ID);
        discussion.setAnnouncementId(TEST_ANNOUNCEMENT_ID);
        discussion.setSenderId(TEST_SENDER_ID);
        discussion.setRecipientId(TEST_RECIPIENT_ID);
        discussion.setCreatedAt(LocalDateTime.now());
        discussion.setUpdatedAt(LocalDateTime.now());
        return discussion;
    }

    /**
     * Create test message.
     *
     * @return test message
     */
    private Message createTestMessage() {
        Message message = new Message();
        message.setId(TEST_MESSAGE_ID);
        message.setDiscussion(testDiscussion);
        message.setAuthorId(TEST_SENDER_ID);
        message.setContent("Test message content");
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}

