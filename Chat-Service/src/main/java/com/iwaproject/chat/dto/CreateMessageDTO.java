package com.iwaproject.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageDTO {

    /**
     * Message content.
     */
    private String content;

    /**
     * Announcement ID (optional - used to auto-create discussion if it doesn't exist).
     */
    private Long announcementId;

    /**
     * Recipient ID (optional - used to auto-create discussion if it doesn't exist).
     */
    private String recipientId;
}

