package com.iwaproject.announcement.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Announcement entity.
 */
@Entity
@Table(name = "announcements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class    Announcement {
    /**
     * Id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owner username.
     */
    @Column(name = "owner_username", nullable = false)
    private String ownerUsername;

    /**
     * Title.
     */
    @Column(nullable = false)
    private String title;

    /**
     * Location.
     */
    @Column(nullable = false)
    private String location;

    /**
     * Description.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Specific instructions.
     */
    @Column(name = "specific_instructions", columnDefinition = "TEXT")
    private String specificInstructions;

    /**
     * Care type.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_type_id", nullable = false)
    private CareType careType;

    /**
     * Start date.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * End date.
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Visit frequency.
     */
    @Column(name = "visit_frequency")
    private String visitFrequency;

    /**
     * Remuneration.
     */
    @Column
    private Float remuneration;

    /**
     * Identity verification required.
     */
    @Column(name = "identity_verification_required", nullable = false)
    private Boolean identityVerificationRequired = false;

    /**
     * Urgent request.
     */
    @Column(name = "urgent_request", nullable = false)
    private Boolean urgentRequest = false;

    /**
     * Status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementStatus status = AnnouncementStatus.PUBLISHED;

    /**
     * Creation date.
     */
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    /**
     * Images.
     */
    @OneToMany(mappedBy = "announcement", fetch = FetchType.LAZY)
    private List<Image> images;

    /**
     * Set creation date before persist.
     */
    @PrePersist
    protected void onCreate() {
        creationDate = LocalDateTime.now();
    }

    /**
     * Announcement status enum.
     */
    public enum AnnouncementStatus {
        /**
         * Published status.
         */
        PUBLISHED,
        /**
         * In progress status.
         */
        IN_PROGRESS,
        /**
         * Completed status.
         */
        COMPLETED
    }

    /**
     * Remove specific instructions from the announcement.
     * Used to hide sensitive information from unauthorized users.
     *
     * @return this announcement instance
     */
    public Announcement removeSpecificInstructions() {
        this.specificInstructions = null;
        return this;
    }
}
