package com.iwaproject.rating.repositories;

import com.iwaproject.rating.entities.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Rating entity.
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    /**
     * Find ratings by recipient ID.
     *
     * @param recipientId the recipient ID
     * @param pageable pagination information
     * @return page of ratings
     */
    @Query("SELECT r FROM Rating r WHERE r.recipientId = :recipientId ORDER BY r.dateAvis DESC")
    Page<Rating> findByRecipientId(
            @Param("recipientId") String recipientId, Pageable pageable);

    /**
     * Find ratings by author ID.
     *
     * @param authorId the author ID
     * @param pageable pagination information
     * @return page of ratings
     */
    @Query("SELECT r FROM Rating r WHERE r.authorId = :authorId ORDER BY r.dateAvis DESC")
    Page<Rating> findByAuthorId(
            @Param("authorId") String authorId, Pageable pageable);

    /**
     * Check if rating exists from author to recipient.
     *
     * @param authorId the author ID
     * @param recipientId the recipient ID
     * @return true if rating exists
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END "
            + "FROM Rating r WHERE r.authorId = :authorId "
            + "AND r.recipientId = :recipientId")
    boolean existsByAuthorIdAndRecipientId(
            @Param("authorId") String authorId,
            @Param("recipientId") String recipientId);

    /**
     * Find rating by author and recipient.
     *
     * @param authorId the author ID
     * @param recipientId the recipient ID
     * @return Optional containing rating if found
     */
    @Query("SELECT r FROM Rating r WHERE r.authorId = :authorId "
            + "AND r.recipientId = :recipientId")
    Optional<Rating> findByAuthorIdAndRecipientId(
            @Param("authorId") String authorId,
            @Param("recipientId") String recipientId);

    /**
     * Calculate average rating for a recipient.
     *
     * @param recipientId the recipient ID
     * @return average rating (or 0.0 if no ratings)
     */
    @Query("SELECT AVG(r.note) FROM Rating r WHERE r.recipientId = :recipientId")
    Double calculateAverageRatingByRecipientId(
            @Param("recipientId") String recipientId);

    /**
     * Count ratings for a recipient.
     *
     * @param recipientId the recipient ID
     * @return number of ratings
     */
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.recipientId = :recipientId")
    Long countByRecipientId(@Param("recipientId") String recipientId);
}

