package com.iwaproject.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a rating.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRatingDTO {

    /**
     * Rating score (1-5).
     */
    @NotNull(message = "Note is required")
    @Min(value = 1, message = "Note must be at least 1")
    @Max(value = 5, message = "Note must be at most 5")
    private Integer note;

    /**
     * Rating comment (optional).
     */
    private String commentaire;
}

