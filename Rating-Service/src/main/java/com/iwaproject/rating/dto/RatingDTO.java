package com.iwaproject.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for rating data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingDTO {

    /**
     * Rating ID.
     */
    private Long id;

    /**
     * Author ID (user who gave the rating).
     */
    private String authorId;

    /**
     * Recipient ID (user who received the rating).
     */
    private String recipientId;

    /**
     * Rating score.
     */
    private Integer note;

    /**
     * Rating comment.
     */
    private String commentaire;

    /**
     * Rating date.
     */
    private LocalDateTime dateAvis;
}

