package com.iwaproject.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new application request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequestDto {

    /**
     * Announcement ID.
     */
    private Integer announcementId;

    /**
     * Guardian ID.
     */
    private Integer guardianId;
}
