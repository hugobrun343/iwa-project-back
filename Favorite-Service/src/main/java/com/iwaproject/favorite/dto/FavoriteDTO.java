package com.iwaproject.favorite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for favorite information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteDTO {

    /**
     * Favorite ID.
     */
    private Integer id;

    /**
     * Guardian username.
     */
    private String guardianUsername;

    /**
     * Announcement ID.
     */
    private Integer announcementId;

    /**
     * Date when the favorite was added.
     */
    private LocalDateTime dateAdded;
}
