package com.iwaproject.chat.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.chat.dto.CreateDiscussionDTO;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
     * Test POST /api/discussions creates new discussion.
     */
    @Test
    @DisplayName("POST /api/discussions creates new discussion")
    void createDiscussion_ok() throws Exception {
        // Given
        CreateDiscussionDTO createDTO = new CreateDiscussionDTO();
        createDTO.setAnnouncementId(TEST_ANNOUNCEMENT_ID);
        createDTO.setRecipientId(TEST_RECIPIENT_ID);
        DiscussionDTO discussion = createTestDiscussionDTO();

        given(chatService.createOrGetDiscussion(
                eq(TEST_USER_ID), eq(TEST_ANNOUNCEMENT_ID),
                eq(TEST_RECIPIENT_ID)))
                .willReturn(discussion);

        // When / Then
        mockMvc.perform(post("/api/discussions")
                .header(X_USERNAME_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TEST_DISCUSSION_ID));
    }

    /**
     * Test POST /api/discussions with missing fields returns bad request.
     */
    @Test
    @DisplayName("POST /api/discussions with missing fields returns bad request")
    void createDiscussion_missingFields_badRequest() throws Exception {
        // Given
        CreateDiscussionDTO createDTO = new CreateDiscussionDTO();
        createDTO.setAnnouncementId(null);
        createDTO.setRecipientId(null);

        // When / Then
        mockMvc.perform(post("/api/discussions")
                .header(X_USERNAME_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
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
                eq("Test message content")))
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
}

