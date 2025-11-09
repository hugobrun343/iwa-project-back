package com.iwaproject.chat.controllers;

import com.iwaproject.chat.dto.MessageDTO;
import com.iwaproject.chat.repositories.DiscussionRepository;
import com.iwaproject.chat.services.ChatService;
import com.iwaproject.chat.services.KafkaLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for WebSocketController.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private DiscussionRepository discussionRepository;

    @Mock
    private KafkaLogService kafkaLogService;

    @InjectMocks
    private WebSocketController webSocketController;

    private static final Long TEST_DISCUSSION_ID = 100L;
    private static final String TEST_AUTHOR_ID = "author-123";
    private static final String TEST_CONTENT = "Test message content";
    private static final Long TEST_ANNOUNCEMENT_ID = 1L;
    private static final String TEST_RECIPIENT_ID = "recipient-456";

    @Test
    @DisplayName("Should handle valid message for existing discussion")
    void testHandleMessage_validMessage() {
        // Arrange
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("authorId", TEST_AUTHOR_ID);
        messagePayload.put("content", TEST_CONTENT);

        MessageDTO expectedMessage = new MessageDTO();
        expectedMessage.setId(200L);
        expectedMessage.setDiscussionId(TEST_DISCUSSION_ID);
        expectedMessage.setAuthorId(TEST_AUTHOR_ID);
        expectedMessage.setContent(TEST_CONTENT);
        expectedMessage.setCreatedAt(LocalDateTime.now());

        given(discussionRepository.isParticipant(TEST_DISCUSSION_ID, TEST_AUTHOR_ID))
                .willReturn(true);
        given(chatService.createMessage(eq(TEST_DISCUSSION_ID), eq(TEST_AUTHOR_ID), 
                eq(TEST_CONTENT), eq(null), eq(null)))
                .willReturn(expectedMessage);

        // Act
        MessageDTO result = webSocketController.handleMessage(
                TEST_DISCUSSION_ID, messagePayload, null);

        // Assert
        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals(TEST_DISCUSSION_ID, result.getDiscussionId());
        assertEquals(TEST_AUTHOR_ID, result.getAuthorId());
        assertEquals(TEST_CONTENT, result.getContent());

        verify(discussionRepository).isParticipant(TEST_DISCUSSION_ID, TEST_AUTHOR_ID);
        verify(chatService).createMessage(TEST_DISCUSSION_ID, TEST_AUTHOR_ID, 
                TEST_CONTENT, null, null);
        verify(kafkaLogService).info(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle message with announcement and recipient IDs")
    void testHandleMessage_withAnnouncementAndRecipient() {
        // Arrange
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("authorId", TEST_AUTHOR_ID);
        messagePayload.put("content", TEST_CONTENT);
        messagePayload.put("announcementId", TEST_ANNOUNCEMENT_ID);
        messagePayload.put("recipientId", TEST_RECIPIENT_ID);

        MessageDTO expectedMessage = new MessageDTO();
        expectedMessage.setId(200L);
        expectedMessage.setDiscussionId(TEST_DISCUSSION_ID);
        expectedMessage.setAuthorId(TEST_AUTHOR_ID);
        expectedMessage.setContent(TEST_CONTENT);
        expectedMessage.setCreatedAt(LocalDateTime.now());

        given(discussionRepository.isParticipant(TEST_DISCUSSION_ID, TEST_AUTHOR_ID))
                .willReturn(true);
        given(chatService.createMessage(eq(TEST_DISCUSSION_ID), eq(TEST_AUTHOR_ID), 
                eq(TEST_CONTENT), eq(TEST_ANNOUNCEMENT_ID), eq(TEST_RECIPIENT_ID)))
                .willReturn(expectedMessage);

        // Act
        MessageDTO result = webSocketController.handleMessage(
                TEST_DISCUSSION_ID, messagePayload, null);

        // Assert
        assertNotNull(result);
        verify(chatService).createMessage(TEST_DISCUSSION_ID, TEST_AUTHOR_ID, 
                TEST_CONTENT, TEST_ANNOUNCEMENT_ID, TEST_RECIPIENT_ID);
    }

    @Test
    @DisplayName("Should return null when user is not participant")
    void testHandleMessage_userNotParticipant() {
        // Arrange
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("authorId", TEST_AUTHOR_ID);
        messagePayload.put("content", TEST_CONTENT);

        given(discussionRepository.isParticipant(TEST_DISCUSSION_ID, TEST_AUTHOR_ID))
                .willReturn(false);

        // Act
        MessageDTO result = webSocketController.handleMessage(
                TEST_DISCUSSION_ID, messagePayload, null);

        // Assert
        assertNull(result);
        verify(discussionRepository).isParticipant(TEST_DISCUSSION_ID, TEST_AUTHOR_ID);
        verify(kafkaLogService).warn(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return null when required fields are missing")
    void testHandleMessage_missingRequiredFields() {
        // Arrange
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("authorId", TEST_AUTHOR_ID);
        messagePayload.put("content", TEST_CONTENT);
        // Missing both discussionId (passed as null) and announcementId/recipientId

        // Act
        MessageDTO result = webSocketController.handleMessage(
                null, messagePayload, null);

        // Assert
        assertNull(result);
        verify(kafkaLogService).warn(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle message creation for new discussion")
    void testHandleMessage_newDiscussion() {
        // Arrange
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("authorId", TEST_AUTHOR_ID);
        messagePayload.put("content", TEST_CONTENT);
        messagePayload.put("announcementId", TEST_ANNOUNCEMENT_ID);
        messagePayload.put("recipientId", TEST_RECIPIENT_ID);

        MessageDTO expectedMessage = new MessageDTO();
        expectedMessage.setId(200L);
        expectedMessage.setDiscussionId(TEST_DISCUSSION_ID);
        expectedMessage.setAuthorId(TEST_AUTHOR_ID);
        expectedMessage.setContent(TEST_CONTENT);
        expectedMessage.setCreatedAt(LocalDateTime.now());

        given(chatService.createMessage(eq(null), eq(TEST_AUTHOR_ID), 
                eq(TEST_CONTENT), eq(TEST_ANNOUNCEMENT_ID), eq(TEST_RECIPIENT_ID)))
                .willReturn(expectedMessage);

        // Act
        MessageDTO result = webSocketController.handleMessage(
                null, messagePayload, null);

        // Assert
        assertNotNull(result);
        assertEquals(200L, result.getId());
        verify(chatService).createMessage(null, TEST_AUTHOR_ID, 
                TEST_CONTENT, TEST_ANNOUNCEMENT_ID, TEST_RECIPIENT_ID);
    }

    @Test
    @DisplayName("Should return null when service throws exception")
    void testHandleMessage_serviceException() {
        // Arrange
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("authorId", TEST_AUTHOR_ID);
        messagePayload.put("content", TEST_CONTENT);

        given(discussionRepository.isParticipant(TEST_DISCUSSION_ID, TEST_AUTHOR_ID))
                .willReturn(true);
        given(chatService.createMessage(eq(TEST_DISCUSSION_ID), eq(TEST_AUTHOR_ID), 
                eq(TEST_CONTENT), eq(null), eq(null)))
                .willThrow(new RuntimeException("Database error"));

        // Act
        MessageDTO result = webSocketController.handleMessage(
                TEST_DISCUSSION_ID, messagePayload, null);

        // Assert
        assertNull(result);
        verify(kafkaLogService).error(anyString(), anyString());
    }
}
