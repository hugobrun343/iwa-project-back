package com.iwaproject.favorite.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.favorite.dto.CreateFavoriteDTO;
import com.iwaproject.favorite.dto.FavoriteDTO;
import com.iwaproject.favorite.services.FavoriteService;
import com.iwaproject.favorite.services.KafkaLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for FavoriteController.
 */
class FavoriteControllerTest {

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
    private FavoriteService favoriteService;
    @Mock
    private KafkaLogService kafkaLogService;

    /**
     * Controller under test.
     */
    @InjectMocks
    private FavoriteController favoriteController;

    /**
     * Test constants.
     */
    private static final String X_USERNAME_HEADER = "X-Username";
    private static final String TEST_USERNAME = "john";
    private static final Integer TEST_ANNOUNCEMENT_ID = 1;

    /**
     * Setup test environment.
     */
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(favoriteController).build();
    }

    /**
     * Test GET /api/favorites returns list.
     */
    @Test
    @DisplayName("GET /api/favorites returns list")
    void getMyFavorites_ok() throws Exception {
        // Given
        FavoriteDTO favoriteDTO = createTestFavoriteDTO();
        when(favoriteService.getFavoritesByGuardian(TEST_USERNAME))
                .thenReturn(List.of(favoriteDTO));

        // When & Then
        mockMvc.perform(get("/api/favorites")
                        .header(X_USERNAME_HEADER, TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].guardianUsername", is(TEST_USERNAME)))
                .andExpect(jsonPath("$[0].announcementId", is(TEST_ANNOUNCEMENT_ID)));

        verify(favoriteService).getFavoritesByGuardian(TEST_USERNAME);
    }

    /**
     * Test GET /api/favorites returns empty list.
     */
    @Test
    @DisplayName("GET /api/favorites returns empty list when no favorites")
    void getMyFavorites_emptyList() throws Exception {
        // Given
        when(favoriteService.getFavoritesByGuardian(TEST_USERNAME))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/favorites")
                        .header(X_USERNAME_HEADER, TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(favoriteService).getFavoritesByGuardian(TEST_USERNAME);
    }

    /**
     * Test GET /api/favorites/check returns true.
     */
    @Test
    @DisplayName("GET /api/favorites/check returns true when favorite exists")
    void checkFavorite_exists() throws Exception {
        // Given
        when(favoriteService.isFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/favorites/check")
                        .header(X_USERNAME_HEADER, TEST_USERNAME)
                        .param("announcementId", TEST_ANNOUNCEMENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite", is(true)));

        verify(favoriteService).isFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Test GET /api/favorites/check returns false.
     */
    @Test
    @DisplayName("GET /api/favorites/check returns false when favorite not exists")
    void checkFavorite_notExists() throws Exception {
        // Given
        when(favoriteService.isFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/favorites/check")
                        .header(X_USERNAME_HEADER, TEST_USERNAME)
                        .param("announcementId", TEST_ANNOUNCEMENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite", is(false)));

        verify(favoriteService).isFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Test POST /api/favorites creates favorite.
     */
    @Test
    @DisplayName("POST /api/favorites creates favorite successfully")
    void addFavorite_ok() throws Exception {
        // Given
        CreateFavoriteDTO createDTO = new CreateFavoriteDTO();
        createDTO.setAnnouncementId(TEST_ANNOUNCEMENT_ID);
        FavoriteDTO favoriteDTO = createTestFavoriteDTO();

        when(favoriteService.addFavorite(eq(TEST_USERNAME), any(CreateFavoriteDTO.class)))
                .thenReturn(favoriteDTO);

        // When & Then
        mockMvc.perform(post("/api/favorites")
                        .header(X_USERNAME_HEADER, TEST_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.guardianUsername", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.announcementId", is(TEST_ANNOUNCEMENT_ID)));

        verify(favoriteService).addFavorite(eq(TEST_USERNAME), any(CreateFavoriteDTO.class));
    }

    /**
     * Test POST /api/favorites when favorite already exists.
     */
    @Test
    @DisplayName("POST /api/favorites returns conflict when favorite exists")
    void addFavorite_alreadyExists() throws Exception {
        // Given
        CreateFavoriteDTO createDTO = new CreateFavoriteDTO();
        createDTO.setAnnouncementId(TEST_ANNOUNCEMENT_ID);

        when(favoriteService.addFavorite(eq(TEST_USERNAME), any(CreateFavoriteDTO.class)))
                .thenThrow(new IllegalStateException("Favorite already exists"));

        // When & Then
        mockMvc.perform(post("/api/favorites")
                        .header(X_USERNAME_HEADER, TEST_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isConflict());

        verify(favoriteService).addFavorite(eq(TEST_USERNAME), any(CreateFavoriteDTO.class));
    }

    /**
     * Test POST /api/favorites with invalid data.
     */
    @Test
    @DisplayName("POST /api/favorites returns bad request with invalid data")
    void addFavorite_invalidData() throws Exception {
        // Given
        String invalidJson = "{}"; // Missing required announcementId

        // When & Then
        mockMvc.perform(post("/api/favorites")
                        .header(X_USERNAME_HEADER, TEST_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test DELETE /api/favorites/{id} deletes favorite.
     */
    @Test
    @DisplayName("DELETE /api/favorites/{id} deletes favorite successfully")
    void removeFavorite_ok() throws Exception {
        // Given
        doNothing().when(favoriteService)
                .removeFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);

        // When & Then
        mockMvc.perform(delete("/api/favorites/{announcementId}", TEST_ANNOUNCEMENT_ID)
                        .header(X_USERNAME_HEADER, TEST_USERNAME))
                .andExpect(status().isNoContent());

        verify(favoriteService).removeFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Test DELETE /api/favorites/{id} when favorite not found.
     */
    @Test
    @DisplayName("DELETE /api/favorites/{id} returns not found when favorite not exists")
    void removeFavorite_notFound() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Favorite not found"))
                .when(favoriteService)
                .removeFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);

        // When & Then
        mockMvc.perform(delete("/api/favorites/{announcementId}", TEST_ANNOUNCEMENT_ID)
                        .header(X_USERNAME_HEADER, TEST_USERNAME))
                .andExpect(status().isNotFound());

        verify(favoriteService).removeFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Test DELETE /api/favorites/{id} when service throws unexpected exception.
     */
    @Test
    @DisplayName("DELETE /api/favorites/{id} returns internal server error on unexpected exception")
    void removeFavorite_unexpectedException() throws Exception {
        // Given
        doThrow(new RuntimeException("Unexpected error"))
                .when(favoriteService)
                .removeFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);

        // When & Then
        mockMvc.perform(delete("/api/favorites/{announcementId}", TEST_ANNOUNCEMENT_ID)
                        .header(X_USERNAME_HEADER, TEST_USERNAME))
                .andExpect(status().isInternalServerError());

        verify(favoriteService).removeFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Test GET /api/favorites/check when service throws exception.
     */
    @Test
    @DisplayName("GET /api/favorites/check returns internal server error on exception")
    void checkFavorite_exception() throws Exception {
        // Given
        when(favoriteService.isFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/favorites/check")
                        .header(X_USERNAME_HEADER, TEST_USERNAME)
                        .param("announcementId", TEST_ANNOUNCEMENT_ID.toString()))
                .andExpect(status().isInternalServerError());

        verify(favoriteService).isFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Create a test favorite DTO.
     *
     * @return test favorite DTO
     */
    private FavoriteDTO createTestFavoriteDTO() {
        FavoriteDTO dto = new FavoriteDTO();
        dto.setId(1);
        dto.setGuardianUsername(TEST_USERNAME);
        dto.setAnnouncementId(TEST_ANNOUNCEMENT_ID);
        dto.setDateAdded(LocalDateTime.now());
        return dto;
    }
}
