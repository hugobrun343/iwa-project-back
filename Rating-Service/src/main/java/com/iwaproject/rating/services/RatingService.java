package com.iwaproject.rating.services;

import com.iwaproject.rating.dto.CreateRatingDTO;
import com.iwaproject.rating.dto.RatingDTO;
import com.iwaproject.rating.entities.Rating;
import com.iwaproject.rating.repositories.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for rating operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    /**
     * Rating repository.
     */
    private final RatingRepository ratingRepository;

    /**
     * Create a new rating.
     *
     * @param authorId the author ID (current user)
     * @param recipientId the recipient ID (user being rated)
     * @param createDTO the rating data
     * @return created rating DTO
     * @throws IllegalArgumentException if rating already exists
     */
    @Transactional
    public RatingDTO createRating(final String authorId,
            final String recipientId, final CreateRatingDTO createDTO) {
        log.info("Creating rating from {} to {}", authorId, recipientId);

        // Check if user is trying to rate themselves
        if (authorId.equals(recipientId)) {
            throw new IllegalArgumentException(
                    "Cannot rate yourself");
        }

        // Check if rating already exists
        if (ratingRepository.existsByAuthorIdAndRecipientId(
                authorId, recipientId)) {
            throw new IllegalArgumentException(
                    "Rating already exists from this author to this recipient");
        }

        // Create new rating
        Rating rating = new Rating();
        rating.setAuthorId(authorId);
        rating.setRecipientId(recipientId);
        rating.setNote(createDTO.getNote());
        rating.setCommentaire(createDTO.getCommentaire());
        rating.setDateAvis(java.time.LocalDateTime.now());

        Rating saved = ratingRepository.save(rating);
        log.info("Created rating: {} from {} to {}", saved.getId(),
                authorId, recipientId);

        return mapToRatingDTO(saved);
    }

    /**
     * Get rating by ID.
     *
     * @param ratingId the rating ID
     * @return rating DTO
     * @throws IllegalArgumentException if rating not found
     */
    public RatingDTO getRatingById(final Long ratingId) {
        log.debug("Fetching rating: {}", ratingId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rating not found: " + ratingId));

        return mapToRatingDTO(rating);
    }

    /**
     * Get ratings by recipient ID.
     *
     * @param recipientId the recipient ID
     * @param page page number (0-based)
     * @param limit page size
     * @return page of ratings
     */
    public Page<RatingDTO> getRatingsByRecipientId(
            final String recipientId, final int page, final int limit) {
        log.debug("Fetching ratings for recipient: {} (page: {}, limit: {})",
                recipientId, page, limit);

        Pageable pageable = PageRequest.of(page, limit);
        Page<Rating> ratings = ratingRepository
                .findByRecipientId(recipientId, pageable);

        return ratings.map(this::mapToRatingDTO);
    }

    /**
     * Get ratings by author ID.
     *
     * @param authorId the author ID
     * @param page page number (0-based)
     * @param limit page size
     * @return page of ratings
     */
    public Page<RatingDTO> getRatingsByAuthorId(
            final String authorId, final int page, final int limit) {
        log.debug("Fetching ratings by author: {} (page: {}, limit: {})",
                authorId, page, limit);

        Pageable pageable = PageRequest.of(page, limit);
        Page<Rating> ratings = ratingRepository
                .findByAuthorId(authorId, pageable);

        return ratings.map(this::mapToRatingDTO);
    }

    /**
     * Update a rating.
     *
     * @param ratingId the rating ID
     * @param authorId the author ID (must match)
     * @param createDTO the updated rating data
     * @return updated rating DTO
     * @throws IllegalArgumentException if rating not found or author doesn't match
     */
    @Transactional
    public RatingDTO updateRating(final Long ratingId,
            final String authorId, final CreateRatingDTO createDTO) {
        log.info("Updating rating: {} by author: {}", ratingId, authorId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rating not found: " + ratingId));

        // Verify author matches
        if (!rating.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException(
                    "Cannot update rating: author mismatch");
        }

        // Update rating
        rating.setNote(createDTO.getNote());
        rating.setCommentaire(createDTO.getCommentaire());

        Rating saved = ratingRepository.save(rating);
        log.info("Updated rating: {}", saved.getId());

        return mapToRatingDTO(saved);
    }

    /**
     * Delete a rating.
     *
     * @param ratingId the rating ID
     * @param authorId the author ID (must match)
     * @throws IllegalArgumentException if rating not found or author doesn't match
     */
    @Transactional
    public void deleteRating(final Long ratingId, final String authorId) {
        log.info("Deleting rating: {} by author: {}", ratingId, authorId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rating not found: " + ratingId));

        // Verify author matches
        if (!rating.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException(
                    "Cannot delete rating: author mismatch");
        }

        ratingRepository.delete(rating);
        log.info("Deleted rating: {}", ratingId);
    }

    /**
     * Get average rating for a recipient.
     *
     * @param recipientId the recipient ID
     * @return average rating (or 0.0 if no ratings)
     */
    public Double getAverageRatingByRecipientId(
            final String recipientId) {
        log.debug("Calculating average rating for recipient: {}", recipientId);

        Double average = ratingRepository
                .calculateAverageRatingByRecipientId(recipientId);

        return average != null ? average : 0.0;
    }

    /**
     * Get rating count for a recipient.
     *
     * @param recipientId the recipient ID
     * @return number of ratings
     */
    public Long getRatingCountByRecipientId(final String recipientId) {
        log.debug("Counting ratings for recipient: {}", recipientId);

        return ratingRepository.countByRecipientId(recipientId);
    }

    /**
     * Get rating by author and recipient.
     *
     * @param authorId the author ID
     * @param recipientId the recipient ID
     * @return rating DTO or null if not found
     */
    public RatingDTO getRatingByAuthorAndRecipient(
            final String authorId, final String recipientId) {
        log.debug("Fetching rating from {} to {}", authorId, recipientId);

        return ratingRepository
                .findByAuthorIdAndRecipientId(authorId, recipientId)
                .map(this::mapToRatingDTO)
                .orElse(null);
    }

    /**
     * Map Rating entity to RatingDTO.
     *
     * @param rating the rating entity
     * @return rating DTO
     */
    private RatingDTO mapToRatingDTO(final Rating rating) {
        RatingDTO dto = new RatingDTO();
        dto.setId(rating.getId());
        dto.setAuthorId(rating.getAuthorId());
        dto.setRecipientId(rating.getRecipientId());
        dto.setNote(rating.getNote());
        dto.setCommentaire(rating.getCommentaire());
        dto.setDateAvis(rating.getDateAvis());
        return dto;
    }
}

