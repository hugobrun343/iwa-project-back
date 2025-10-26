package com.iwaproject.announcement.dto;

import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Announcement response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateAnnouncementResponseDto {
    /**
     * Id.
     */
    private Long id;
    /**
     * Owner id.
     */
    private Long ownerId;
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
     * Images base64 encoded.
     */
    private String[] images;

    /**
     * Specific images base64 encoded.
     */
    private String[] specificImages;
}
