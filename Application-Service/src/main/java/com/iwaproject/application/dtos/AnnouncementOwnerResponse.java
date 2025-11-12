package com.iwaproject.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for announcement owner response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementOwnerResponse {

    /**
     * Unique request ID for tracking the request/response.
     */
    private String requestId;

    /**
     * The announcement ID.
     */
    private Integer announcementId;

    /**
     * The owner username of the announcement.
     */
    private String ownerUsername;
}
