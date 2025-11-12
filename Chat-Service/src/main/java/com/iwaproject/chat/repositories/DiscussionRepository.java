package com.iwaproject.chat.repositories;

import com.iwaproject.chat.entities.Discussion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Discussion entity.
 */
@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    /**
     * Find discussions where user is sender or recipient.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of discussions
     */
    @Query("SELECT d FROM Discussion d WHERE d.senderId = :userId OR d.recipientId = :userId ORDER BY d.updatedAt DESC")
    Page<Discussion> findBySenderIdOrRecipientId(
            @Param("userId") String userId, Pageable pageable);

    /**
     * Find discussion by announcement ID, sender ID and recipient ID.
     *
     * @param announcementId the announcement ID
     * @param senderId the sender ID
     * @param recipientId the recipient ID
     * @return Optional containing discussion if found
     */
    @Query("SELECT d FROM Discussion d WHERE d.announcementId = :announcementId AND ((d.senderId = :senderId AND d.recipientId = :recipientId) OR (d.senderId = :recipientId AND d.recipientId = :senderId))")
    Optional<Discussion> findByAnnouncementIdAndParticipants(
            @Param("announcementId") Long announcementId,
            @Param("senderId") String senderId,
            @Param("recipientId") String recipientId);

    /**
     * Check if user is participant in discussion.
     *
     * @param discussionId the discussion ID
     * @param userId the user ID
     * @return true if user is participant
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Discussion d WHERE d.id = :discussionId AND (d.senderId = :userId OR d.recipientId = :userId)")
    boolean isParticipant(@Param("discussionId") Long discussionId,
            @Param("userId") String userId);
}

