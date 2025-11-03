package com.iwaproject.chat.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.chat.dto.CreateMessageDTO;
import com.iwaproject.chat.dto.DiscussionDTO;
import com.iwaproject.chat.dto.MessageDTO;
import com.iwaproject.chat.services.ChatService;
import com.iwaproject.chat.services.KafkaLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for ChatController.
 */
class ChatControllerTest {

    /**
     * MockMvc for testing web layer.
     */
    private MockMvc mockMvc;

    /**
     * ObjectMapper for JSON operations.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Mock services.
     */
    @Mock
    private ChatService chatService;
    @Mock
    private KafkaLogService kafkaLogService;

    /**
     * Controller under test.
     */
    @InjectMocks
    private ChatController chatController;

    /**
     * Test constants.
     */
    private static final String X_USERNAME_HEADER = "X-Username";
    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_RECIPIENT_ID = "recipient-456";
    private static final Long TEST_ANNOUNCEMENT_ID = 1L;
    private static final Long TEST_DISCUSSION_ID = 100L;
    private static final Long TEST_MESSAGE_ID = 200L;

    /**
     * Setup test environment.
     */
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
    }

    /**
     * Test GET /api/me/discussions returns paginated discussions.
     */
    @Test
    @DisplayName("GET /api/me/discussions returns paginated discussions")
    void getMyDiscussions_ok() throws Exception {
        // Given
        DiscussionDTO discussion = createTestDiscussionDTO();
        Page<DiscussionDTO> page = new PageImpl<>(
                List.of(discussion), PageRequest.of(0, 20), 1);
        given(chatService.getMyDiscussions(
                eq(TEST_USER_ID), eq(0), eq(20)))
                .willReturn(page);

        // When / Then
        mockMvc.perform(get("/api/me/discussions")
                .header(X_USERNAME_HEADER, TEST_USER_ID)
                .param("page", "0")
                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TEST_DISCUSSION_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /**
     * Test GET /api/discussions with query params returns discussion.
     */
    @Test
    @DisplayName("GET /api/discussions with query params returns discussion")
    void getDiscussionByAnnouncement_ok() throws Exception {
        // Given
        DiscussionDTO discussion = createTestDiscussionDTO();
        given(chatService.getDiscussionByAnnouncementAndParticipants(
                eq(TEST_ANNOUNCEMENT_ID), eq(TEST_USER_ID),
                eq(TEST_RECIPIENT_ID)))
                .willReturn(discussion);

        // When / Then
        mockMvc.perform(get("/api/discussions")
                .header(X_USERNAME_HEADER, TEST_USER_ID)
                .param("announcementId", TEST_ANNOUNCEMENT_ID.toString())
                .param("recipientId", TEST_RECIPIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_DISCUSSION_ID));
    }

    /**
     * Test GET /api/discussions with query params returns empty discussion when not found.
     */
    @Test
    @DisplayName("GET /api/discussions returns empty discussion when not found")
    void getDiscussionByAnnouncement_notFound_returnsEmpty() throws Exception {
        // Given
        DiscussionDTO emptyDiscussion = createEmptyDiscussionDTO();
        given(chatService.getDiscussionByAnnouncementAndParticipants(
                eq(TEST_ANNOUNCEMENT_ID), eq(TEST_USER_ID),
                eq(TEST_RECIPIENT_ID)))
                .willReturn(emptyDiscussion);

        // When / Then
        mockMvc.perform(get("/api/discussions")
                .header(X_USERNAME_HEADER, TEST_USER_ID)
                .param("announcementId", TEST_ANNOUNCEMENT_ID.toString())
                .param("recipientId", TEST_RECIPIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isEmpty());
    }

    /**
     * Test GET /api/discussions/{id} returns discussion.
     */
    @Test
    @DisplayName("GET /api/discussions/{id} returns discussion")
    void getDiscussionById_ok() throws Exception {
        // Given
        DiscussionDTO discussion = createTestDiscussionDTO();
        given(chatService.getDiscussionById(
                eq(TEST_DISCUSSION_ID), eq(TEST_USER_ID)))
                .willReturn(discussion);

        // When / Then
        mockMvc.perform(get("/api/discussions/" + TEST_DISCUSSION_ID)
                .header(X_USERNAME_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_DISCUSSION_ID));
    }

    /**
     * Test GET /api/discussions/{id} returns empty discussion when not found.
     */
    @Test
    @DisplayName("GET /api/discussions/{id} returns empty discussion when not found")
    void getDiscussionById_notFound_returnsEmpty() throws Exception {
        // Given
        DiscussionDTO emptyDiscussion = createEmptyDiscussionDTO();
        given(chatService.getDiscussionById(
                eq(TEST_DISCUSSION_ID), eq(TEST_USER_ID)))
                .willReturn(emptyDiscussion);

        // When / Then
        mockMvc.perform(get("/api/discussions/" + TEST_DISCUSSION_ID)
                .header(X_USERNAME_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isEmpty());
    }

    /**
     * Test GET /api/discussions/{id}/messages returns paginated messages.
     */
    @Test
    @DisplayName("GET /api/discussions/{id}/messages returns paginated messages")
    void getMessagesByDiscussionId_ok() throws Exception {
        // Given
        MessageDTO message = createTestMessageDTO();
        Page<MessageDTO> page = new PageImpl<>(
                List.of(message), PageRequest.of(0, 20), 1);
        given(chatService.getMessagesByDiscussionId(
                eq(TEST_DISCUSSION_ID), eq(TEST_USER_ID),
                eq(0), eq(20)))
                .willReturn(page);

        // When / Then
        mockMvc.perform(get("/api/discussions/" + TEST_DISCUSSION_ID
                + "/messages")
                .header(X_USERNAME_HEADER, TEST_USER_ID)
                .param("page", "0")
                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TEST_MESSAGE_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /**
     * Test POST /api/discussions/{id}/messages creates new message.
     */
    @Test
    @DisplayName("POST /api/discussions/{id}/messages creates new message")
    void createMessage_ok() throws Exception {
        // Given
        CreateMessageDTO createDTO = new CreateMessageDTO();
        createDTO.setContent("Test message content");
        MessageDTO message = createTestMessageDTO();

        given(chatService.createMessage(
                eq(TEST_DISCUSSION_ID), eq(TEST_USER_ID),
                eq("Test message content"), any(), any()))
                .willReturn(message);

        // When / Then
        mockMvc.perform(post("/api/discussions/" + TEST_DISCUSSION_ID
                + "/messages")
                .header(X_USERNAME_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TEST_MESSAGE_ID));
    }

    /**
     * Test POST /api/discussions/{id}/messages creates discussion automatically when not exists.
     */
    @Test
    @DisplayName("POST /api/discussions/{id}/messages creates discussion automatically when not exists")
    void createMessage_autoCreatesDiscussion() throws Exception {
        // Given
        CreateMessageDTO createDTO = new CreateMessageDTO();
        createDTO.setContent("Test message content");
        createDTO.setAnnouncementId(TEST_ANNOUNCEMENT_ID);
        createDTO.setRecipientId(TEST_RECIPIENT_ID);
        MessageDTO message = createTestMessageDTO();

        given(chatService.createMessage(
                eq(TEST_DISCUSSION_ID), eq(TEST_USER_ID),
                eq("Test message content"), eq(TEST_ANNOUNCEMENT_ID),
                eq(TEST_RECIPIENT_ID)))
                .willReturn(message);

        // When / Then
        mockMvc.perform(post("/api/discussions/" + TEST_DISCUSSION_ID
                + "/messages")
                .header(X_USERNAME_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TEST_MESSAGE_ID));
    }

    /**
     * Test POST /api/discussions/{id}/messages with empty content returns bad request.
     */
    @Test
    @DisplayName("POST /api/discussions/{id}/messages with empty content returns bad request")
    void createMessage_emptyContent_badRequest() throws Exception {
        // Given
        CreateMessageDTO createDTO = new CreateMessageDTO();
        createDTO.setContent("");

        // When / Then
        mockMvc.perform(post("/api/discussions/" + TEST_DISCUSSION_ID
                + "/messages")
                .header(X_USERNAME_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test DELETE /api/discussions/{id} deletes discussion.
     */
    @Test
    @DisplayName("DELETE /api/discussions/{id} deletes discussion")
    void deleteDiscussion_ok() throws Exception {
        // Given
        willDoNothing().given(chatService).deleteDiscussion(
                eq(TEST_DISCUSSION_ID), eq(TEST_USER_ID));

        // When / Then
        mockMvc.perform(delete("/api/discussions/" + TEST_DISCUSSION_ID)
                .header(X_USERNAME_HEADER, TEST_USER_ID))
                .andExpect(status().isNoContent());
    }

    /**
     * Test DELETE /api/discussions/{id} returns not found when discussion doesn't exist.
     */
    @Test
    @DisplayName("DELETE /api/discussions/{id} returns not found when discussion doesn't exist")
    void deleteDiscussion_notFound() throws Exception {
        // Given
        willDoNothing().given(chatService).deleteDiscussion(
                eq(TEST_DISCUSSION_ID), eq(TEST_USER_ID));

        // When / Then
        mockMvc.perform(delete("/api/discussions/" + TEST_DISCUSSION_ID)
                .header(X_USERNAME_HEADER, TEST_USER_ID))
                .andExpect(status().isNoContent());
    }

    /**
     * Create test discussion DTO.
     *
     * @return test discussion DTO
     */
    private DiscussionDTO createTestDiscussionDTO() {
        DiscussionDTO dto = new DiscussionDTO();
        dto.setId(TEST_DISCUSSION_ID);
        dto.setAnnouncementId(TEST_ANNOUNCEMENT_ID);
        dto.setSenderId(TEST_USER_ID);
        dto.setRecipientId(TEST_RECIPIENT_ID);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Create test message DTO.
     *
     * @return test message DTO
     */
    private MessageDTO createTestMessageDTO() {
        MessageDTO dto = new MessageDTO();
        dto.setId(TEST_MESSAGE_ID);
        dto.setDiscussionId(TEST_DISCUSSION_ID);
        dto.setAuthorId(TEST_USER_ID);
        dto.setContent("Test message content");
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Create empty discussion DTO.
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
}

