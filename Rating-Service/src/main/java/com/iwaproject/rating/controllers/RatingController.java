package com.iwaproject.rating.controllers;

import com.iwaproject.rating.dto.CreateRatingDTO;
import com.iwaproject.rating.dto.RatingDTO;
import com.iwaproject.rating.services.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST controller for rating operations.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RatingController {

    /**
     * Rating service.
     */
    private final RatingService ratingService;

    /**
     * Create a new rating.
     *
     * @param userId the user ID (from token, will be the author)
     * @param recipientId the recipient ID (user being rated)
     * @param createDTO the rating data
     * @return created rating
     */
    @PostMapping("/ratings/{recipientId}")
    public ResponseEntity<RatingDTO> createRating(
            @RequestHeader("X-Username") final String userId,
            @PathVariable("recipientId") final String recipientId,
            @Valid @RequestBody final CreateRatingDTO createDTO) {

        log.info("POST /ratings/{} - User: {}", recipientId, userId);

        try {
            RatingDTO rating = ratingService.createRating(
                    userId, recipientId, createDTO);

            return ResponseEntity.created(
                            URI.create("/api/ratings/" + rating.getId()))
                    .body(rating);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to create rating: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get rating by ID.
     *
     * @param id the rating ID
     * @return rating
     */
    @GetMapping("/ratings/{id}")
    public ResponseEntity<RatingDTO> getRatingById(
            @PathVariable("id") final Long id) {

        log.info("GET /ratings/{}", id);

        try {
            RatingDTO rating = ratingService.getRatingById(id);
            return ResponseEntity.ok(rating);
        } catch (IllegalArgumentException e) {
            log.warn("Rating not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get rating: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get ratings by recipient ID.
     *
     * @param recipientId the recipient ID
     * @param page page number (optional, default: 0)
     * @param limit page size (optional, default: 20)
     * @return page of ratings
     */
    @GetMapping("/ratings/recipient/{recipientId}")
    public ResponseEntity<Page<RatingDTO>> getRatingsByRecipientId(
            @PathVariable("recipientId") final String recipientId,
            @RequestParam(value = "page", required = false,
                    defaultValue = "0") final int page,
            @RequestParam(value = "limit", required = false,
                    defaultValue = "20") final int limit) {

        log.info("GET /ratings/recipient/{} - page: {}, limit: {}",
                recipientId, page, limit);

        try {
            Page<RatingDTO> ratings = ratingService
                    .getRatingsByRecipientId(recipientId, page, limit);
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            log.error("Failed to get ratings: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get ratings by author ID.
     *
     * @param authorId the author ID
     * @param page page number (optional, default: 0)
     * @param limit page size (optional, default: 20)
     * @return page of ratings
     */
    @GetMapping("/ratings/author/{authorId}")
    public ResponseEntity<Page<RatingDTO>> getRatingsByAuthorId(
            @PathVariable("authorId") final String authorId,
            @RequestParam(value = "page", required = false,
                    defaultValue = "0") final int page,
            @RequestParam(value = "limit", required = false,
                    defaultValue = "20") final int limit) {

        log.info("GET /ratings/author/{} - page: {}, limit: {}",
                authorId, page, limit);

        try {
            Page<RatingDTO> ratings = ratingService
                    .getRatingsByAuthorId(authorId, page, limit);
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            log.error("Failed to get ratings: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get average rating for a recipient.
     *
     * @param recipientId the recipient ID
     * @return average rating
     */
    @GetMapping("/ratings/recipient/{recipientId}/average")
    public ResponseEntity<Double> getAverageRating(
            @PathVariable("recipientId") final String recipientId) {

        log.info("GET /ratings/recipient/{}/average", recipientId);

        try {
            Double average = ratingService
                    .getAverageRatingByRecipientId(recipientId);
            return ResponseEntity.ok(average);
        } catch (Exception e) {
            log.error("Failed to get average rating: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get rating count for a recipient.
     *
     * @param recipientId the recipient ID
     * @return rating count
     */
    @GetMapping("/ratings/recipient/{recipientId}/count")
    public ResponseEntity<Long> getRatingCount(
            @PathVariable("recipientId") final String recipientId) {

        log.info("GET /ratings/recipient/{}/count", recipientId);

        try {
            Long count = ratingService
                    .getRatingCountByRecipientId(recipientId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Failed to get rating count: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update a rating.
     *
     * @param id the rating ID
     * @param userId the user ID (from token, must be author)
     * @param createDTO the updated rating data
     * @return updated rating
     */
    @PutMapping("/ratings/{id}")
    public ResponseEntity<RatingDTO> updateRating(
            @PathVariable("id") final Long id,
            @RequestHeader("X-Username") final String userId,
            @Valid @RequestBody final CreateRatingDTO createDTO) {

        log.info("PUT /ratings/{} - User: {}", id, userId);

        try {
            RatingDTO rating = ratingService.updateRating(
                    id, userId, createDTO);
            return ResponseEntity.ok(rating);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Failed to update rating: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a rating.
     *
     * @param id the rating ID
     * @param userId the user ID (from token, must be author)
     * @return no content if successful
     */
    @DeleteMapping("/ratings/{id}")
    public ResponseEntity<Void> deleteRating(
            @PathVariable("id") final Long id,
            @RequestHeader("X-Username") final String userId) {

        log.info("DELETE /ratings/{} - User: {}", id, userId);

        try {
            ratingService.deleteRating(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Failed to delete rating: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

