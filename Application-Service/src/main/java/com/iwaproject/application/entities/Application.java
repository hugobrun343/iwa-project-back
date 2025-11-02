package com.iwaproject.application.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing an application (candidature).
 */
@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class Application {

    /**
     * Maximum length for status field in database.
     */
    private static final int STATUS_MAX_LENGTH = 20;

    /**
     * Application ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Announcement ID reference.
     */
    @Column(name = "annonce_id", nullable = false)
    private Integer announcementId;

    /**
     * Guardian ID reference.
     */
    @Column(name = "guardian_username", nullable = false)
    private String guardianUsername;

    /**
     * Application status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private ApplicationStatus status;

    /**
     * Application submission date.
     */
    @Column(name = "date_candidature", nullable = false)
    private LocalDateTime applicationDate;

    /**
     * Sets default values before persisting.
     */
    @PrePersist
    protected void onCreate() {
        if (applicationDate == null) {
            applicationDate = LocalDateTime.now();
        }
        if (status == null) {
            status = ApplicationStatus.SENT;
        }
    }
}
