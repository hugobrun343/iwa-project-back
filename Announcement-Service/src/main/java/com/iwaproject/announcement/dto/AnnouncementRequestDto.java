package com.iwaproject.announcement.dto;

import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating/updating an Announcement.
 * Uses care type label instead of ID for easier API usage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRequestDto {
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
     * Care type label.
     */
    private String careTypeLabel;
    
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
     * Public images.
     */
    private List<ImageDto> publicImages;

    /**
     * Specific images.
     */
    private List<ImageDto> specificImages;
}
