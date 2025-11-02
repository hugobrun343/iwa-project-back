package com.iwaproject.announcement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting announcement owner information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementOwnerRequest {

    /**
     * Unique request ID for tracking the request/response.
     */
    private String requestId;

    /**
     * The announcement ID to get the owner for.
     */
    private Long announcementId;
}
