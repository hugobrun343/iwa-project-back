package com.iwaproject.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a discussion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDiscussionDTO {

    /**
     * Announcement ID (required - each discussion must be linked to an announcement).
     */
    private Long announcementId;

    /**
     * Recipient ID (user to start discussion with).
     */
    private String recipientId;
}

