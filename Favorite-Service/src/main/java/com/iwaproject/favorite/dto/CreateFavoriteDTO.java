package com.iwaproject.favorite.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new favorite.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFavoriteDTO {

    /**
     * Announcement ID to add to favorites.
     */
    @NotNull(message = "Announcement ID is required")
    private Integer announcementId;
}
