package com.iwaproject.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving application verification requests from
 * Announcement-Service.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationVerificationRequest {

    /**
     * Unique correlation ID for matching requests with responses.
     */
    private String requestId;

    /**
     * Username of the user to verify.
     */
    private String username;

    /**
     * ID of the announcement to check.
     */
    private Long announcementId;
}
