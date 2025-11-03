package com.iwaproject.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for message information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    /**
     * Message ID.
     */
    private Long id;

    /**
     * Discussion ID.
     */
    private Long discussionId;

    /**
     * Author ID (user who sent the message).
     */
    private String authorId;

    /**
     * Message content.
     */
    private String content;

    /**
     * Creation date.
     */
    private LocalDateTime createdAt;
}

