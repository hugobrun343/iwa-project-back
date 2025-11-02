package com.iwaproject.chat.repositories;

import com.iwaproject.chat.entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find messages by discussion ID, ordered by creation date.
     *
     * @param discussionId the discussion ID
     * @param pageable pagination information
     * @return page of messages
     */
    @Query("SELECT m FROM Message m WHERE m.discussion.id = :discussionId ORDER BY m.createdAt ASC")
    Page<Message> findByDiscussionId(@Param("discussionId") Long discussionId,
            Pageable pageable);

    /**
     * Find all messages by discussion ID, ordered by creation date.
     *
     * @param discussionId the discussion ID
     * @return list of messages
     */
    @Query("SELECT m FROM Message m WHERE m.discussion.id = :discussionId ORDER BY m.createdAt ASC")
    List<Message> findAllByDiscussionId(@Param("discussionId") Long discussionId);
}

