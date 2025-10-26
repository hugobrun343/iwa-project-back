package com.iwaproject.application.dtos;

import com.iwaproject.application.entities.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for application response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponseDto {

    /**
     * Application ID.
     */
    private Integer id;

    /**
     * Announcement ID.
     */
    private Integer announcementId;

    /**
     * Guardian ID.
     */
    private Integer guardianId;

    /**
     * Application status.
     */
    private ApplicationStatus status;

    /**
     * Application submission date.
     */
    private LocalDateTime applicationDate;
}
