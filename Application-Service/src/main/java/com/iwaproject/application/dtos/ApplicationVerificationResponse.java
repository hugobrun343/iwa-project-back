package com.iwaproject.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending application verification responses to Announcement-Service.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationVerificationResponse {

    /**
     * Correlation ID matching the request.
     */
    private String requestId;

    /**
     * Username of the user that was verified.
     */
    private String username;

    /**
     * ID of the announcement that was checked.
     */
    private Long announcementId;

    /**
     * Whether the user has an accepted application.
     */
    private boolean accepted;
}
