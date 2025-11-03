package com.iwaproject.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for discussion information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionDTO {

    /**
     * Discussion ID.
     */
    private Long id;

    /**
     * Announcement ID.
     */
    private Long announcementId;

    /**
     * Sender ID.
     */
    private String senderId;

    /**
     * Recipient ID.
     */
    private String recipientId;

    /**
     * Creation date.
     */
    private LocalDateTime createdAt;

    /**
     * Last update date.
     */
    private LocalDateTime updatedAt;
}

