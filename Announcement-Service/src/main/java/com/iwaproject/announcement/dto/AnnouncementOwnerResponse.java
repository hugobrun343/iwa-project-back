package com.iwaproject.announcement.dto;

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
    private Long announcementId;

    /**
     * The owner username of the announcement.
     */
    private String ownerUsername;
}
