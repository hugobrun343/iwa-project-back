package com.iwaproject.announcement.dto;

import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Announcement response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponseDto {
    /**
     * Id.
     */
    private Long id;

    /**
     * Owner username.
     */
    private String ownerUsername;

    /**
     * Title.
     */
    private String title;

    /**
     * Location.
     */
    private String location;

    /**
     * Description.
     */
    private String description;

    /**
     * Specific instructions.
     */
    private String specificInstructions;

    /**
     * Care type.
     */
    private CareTypeDto careType;

    /**
     * Start date.
     */
    private LocalDate startDate;

    /**
     * End date.
     */
    private LocalDate endDate;

    /**
     * Visit frequency.
     */
    private String visitFrequency;

    /**
     * Remuneration.
     */
    private Float remuneration;

    /**
     * Identity verification required.
     */
    private Boolean identityVerificationRequired;

    /**
     * Urgent request.
     */
    private Boolean urgentRequest;

    /**
     * Status.
     */
    private AnnouncementStatus status;

    /**
     * Creation date.
     */
    private LocalDateTime creationDate;

    /**
     * Public images.
     */
    private List<ImageDto> publicImages;

    /**
     * Specific images.
     */
    private List<ImageDto> specificImages;
}
