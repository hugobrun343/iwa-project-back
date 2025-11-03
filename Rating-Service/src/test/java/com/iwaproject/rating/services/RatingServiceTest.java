package com.iwaproject.rating.services;

import com.iwaproject.rating.dto.CreateRatingDTO;
import com.iwaproject.rating.dto.RatingDTO;
import com.iwaproject.rating.entities.Rating;
import com.iwaproject.rating.repositories.RatingRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RatingService.
 */
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    /**
     * Test constants.
     */
    private static final Long TEST_RATING_ID = 1L;
    private static final String TEST_AUTHOR_ID = "author1";
    private static final String TEST_RECIPIENT_ID = "recipient1";
    private static final Integer TEST_NOTE = 5;
    private static final String TEST_COMMENTAIRE = "Great service!";

    /**
     * Rating repository mock.
     */
    @Mock
    private RatingRepository ratingRepository;

    /**
     * Rating service (under test).
     */
    @InjectMocks
    private RatingService ratingService;

    /**
     * Test rating entity.
     */
    private Rating testRating;

    /**
     * Test create rating DTO.
     */
    private CreateRatingDTO testCreateDTO;

    /**
     * Setup before each test.
     */
    @BeforeEach
    void setUp() {
        testRating = new Rating();
        testRating.setId(TEST_RATING_ID);
        testRating.setAuthorId(TEST_AUTHOR_ID);
        testRating.setRecipientId(TEST_RECIPIENT_ID);
        testRating.setNote(TEST_NOTE);
        testRating.setCommentaire(TEST_COMMENTAIRE);
        testRating.setDateAvis(LocalDateTime.now());

        testCreateDTO = new CreateRatingDTO();
        testCreateDTO.setNote(TEST_NOTE);
        testCreateDTO.setCommentaire(TEST_COMMENTAIRE);
    }

    @Test
    @DisplayName("createRating should create a new rating")
    void createRating_shouldCreateNewRating() {
        // Given
        when(ratingRepository.existsByAuthorIdAndRecipientId(
                eq(TEST_AUTHOR_ID), eq(TEST_RECIPIENT_ID)))
                .thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);

        // When
        RatingDTO result = ratingService.createRating(
                TEST_AUTHOR_ID, TEST_RECIPIENT_ID, testCreateDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_RATING_ID, result.getId());
        assertEquals(TEST_AUTHOR_ID, result.getAuthorId());
        assertEquals(TEST_RECIPIENT_ID, result.getRecipientId());
        assertEquals(TEST_NOTE, result.getNote());
        assertEquals(TEST_COMMENTAIRE, result.getCommentaire());
    }

    @Test
    @DisplayName("createRating should throw when rating self")
    void createRating_shouldThrowWhenRatingSelf() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.createRating(TEST_AUTHOR_ID, TEST_AUTHOR_ID, testCreateDTO);
        });
    }

    @Test
    @DisplayName("createRating should throw when rating already exists")
    void createRating_shouldThrowWhenRatingExists() {
        // Given
        when(ratingRepository.existsByAuthorIdAndRecipientId(
                eq(TEST_AUTHOR_ID), eq(TEST_RECIPIENT_ID)))
                .thenReturn(true);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.createRating(TEST_AUTHOR_ID, TEST_RECIPIENT_ID, testCreateDTO);
        });
    }

    @Test
    @DisplayName("getRatingById should return rating")
    void getRatingById_shouldReturnRating() {
        // Given
        when(ratingRepository.findById(TEST_RATING_ID))
                .thenReturn(Optional.of(testRating));

        // When
        RatingDTO result = ratingService.getRatingById(TEST_RATING_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_RATING_ID, result.getId());
    }

    @Test
    @DisplayName("getRatingById should throw when not found")
    void getRatingById_shouldThrowWhenNotFound() {
        // Given
        when(ratingRepository.findById(TEST_RATING_ID))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.getRatingById(TEST_RATING_ID);
        });
    }

    @Test
    @DisplayName("getRatingsByRecipientId should return page of ratings")
    void getRatingsByRecipientId_shouldReturnPage() {
        // Given
        List<Rating> ratings = new ArrayList<>();
        ratings.add(testRating);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Rating> page = new PageImpl<>(ratings, pageable, 1);

        when(ratingRepository.findByRecipientId(eq(TEST_RECIPIENT_ID), any(Pageable.class)))
                .thenReturn(page);

        // When
        Page<RatingDTO> result = ratingService.getRatingsByRecipientId(
                TEST_RECIPIENT_ID, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getAverageRatingByRecipientId should return average")
    void getAverageRatingByRecipientId_shouldReturnAverage() {
        // Given
        when(ratingRepository.calculateAverageRatingByRecipientId(TEST_RECIPIENT_ID))
                .thenReturn(4.5);

        // When
        Double result = ratingService.getAverageRatingByRecipientId(TEST_RECIPIENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(4.5, result);
    }

    @Test
    @DisplayName("getAverageRatingByRecipientId should return 0.0 when no ratings")
    void getAverageRatingByRecipientId_shouldReturnZeroWhenNoRatings() {
        // Given
        when(ratingRepository.calculateAverageRatingByRecipientId(TEST_RECIPIENT_ID))
                .thenReturn(null);

        // When
        Double result = ratingService.getAverageRatingByRecipientId(TEST_RECIPIENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(0.0, result);
    }
}

