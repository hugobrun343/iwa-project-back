package com.iwaproject.rating.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.rating.dto.CreateRatingDTO;
import com.iwaproject.rating.dto.RatingDTO;
import com.iwaproject.rating.services.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for RatingController.
 */
@WebMvcTest(RatingController.class)
class RatingControllerTest {

    /**
     * Test constants.
     */
    private static final Long TEST_RATING_ID = 1L;
    private static final String TEST_AUTHOR_ID = "author1";
    private static final String TEST_RECIPIENT_ID = "recipient1";
    private static final String X_USERNAME_HEADER = "X-Username";

    /**
     * MockMvc for testing.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Object mapper for JSON.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Rating service mock.
     */
    @MockBean
    private RatingService ratingService;

    /**
     * Test rating DTO.
     */
    private RatingDTO testRatingDTO;

    /**
     * Test create rating DTO.
     */
    private CreateRatingDTO testCreateDTO;

    /**
     * Setup before each test.
     */
    @BeforeEach
    void setUp() {
        testRatingDTO = new RatingDTO();
        testRatingDTO.setId(TEST_RATING_ID);
        testRatingDTO.setAuthorId(TEST_AUTHOR_ID);
        testRatingDTO.setRecipientId(TEST_RECIPIENT_ID);
        testRatingDTO.setNote(5);
        testRatingDTO.setCommentaire("Great service!");
        testRatingDTO.setDateAvis(LocalDateTime.now());

        testCreateDTO = new CreateRatingDTO();
        testCreateDTO.setNote(5);
        testCreateDTO.setCommentaire("Great service!");
    }

    @Test
    @DisplayName("POST /api/ratings/{recipientId} creates rating")
    void createRating_ok() throws Exception {
        // Given
        when(ratingService.createRating(
                eq(TEST_AUTHOR_ID), eq(TEST_RECIPIENT_ID), any(CreateRatingDTO.class)))
                .thenReturn(testRatingDTO);

        // When / Then
        mockMvc.perform(post("/api/ratings/" + TEST_RECIPIENT_ID)
                .header(X_USERNAME_HEADER, TEST_AUTHOR_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TEST_RATING_ID))
                .andExpect(jsonPath("$.note").value(5));
    }

    @Test
    @DisplayName("GET /api/ratings/{id} returns rating")
    void getRatingById_ok() throws Exception {
        // Given
        when(ratingService.getRatingById(TEST_RATING_ID))
                .thenReturn(testRatingDTO);

        // When / Then
        mockMvc.perform(get("/api/ratings/" + TEST_RATING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_RATING_ID));
    }

    @Test
    @DisplayName("GET /api/ratings/recipient/{recipientId} returns page of ratings")
    void getRatingsByRecipientId_ok() throws Exception {
        // Given
        List<RatingDTO> ratings = new ArrayList<>();
        ratings.add(testRatingDTO);
        Page<RatingDTO> page = new PageImpl<>(ratings);

        when(ratingService.getRatingsByRecipientId(eq(TEST_RECIPIENT_ID), eq(0), eq(20)))
                .thenReturn(page);

        // When / Then
        mockMvc.perform(get("/api/ratings/recipient/" + TEST_RECIPIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TEST_RATING_ID));
    }

    @Test
    @DisplayName("GET /api/ratings/recipient/{recipientId}/average returns average")
    void getAverageRating_ok() throws Exception {
        // Given
        when(ratingService.getAverageRatingByRecipientId(TEST_RECIPIENT_ID))
                .thenReturn(4.5);

        // When / Then
        mockMvc.perform(get("/api/ratings/recipient/" + TEST_RECIPIENT_ID + "/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4.5));
    }

    @Test
    @DisplayName("PUT /api/ratings/{id} updates rating")
    void updateRating_ok() throws Exception {
        // Given
        when(ratingService.updateRating(
                eq(TEST_RATING_ID), eq(TEST_AUTHOR_ID), any(CreateRatingDTO.class)))
                .thenReturn(testRatingDTO);

        // When / Then
        mockMvc.perform(put("/api/ratings/" + TEST_RATING_ID)
                .header(X_USERNAME_HEADER, TEST_AUTHOR_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_RATING_ID));
    }

    @Test
    @DisplayName("DELETE /api/ratings/{id} deletes rating")
    void deleteRating_ok() throws Exception {
        // Given
        willDoNothing().given(ratingService)
                .deleteRating(eq(TEST_RATING_ID), eq(TEST_AUTHOR_ID));

        // When / Then
        mockMvc.perform(delete("/api/ratings/" + TEST_RATING_ID)
                .header(X_USERNAME_HEADER, TEST_AUTHOR_ID))
                .andExpect(status().isNoContent());
    }
}

