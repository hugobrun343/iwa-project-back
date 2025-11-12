package com.iwaproject.favorite.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Favorite entity - stores user's favorite announcements.
 */
@Entity
@Table(name = "favorites")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {

    /**
     * Maximum length for username.
     */
    private static final int MAX_USERNAME_LENGTH = 255;

    /**
     * Unique identifier for the favorite.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /**
     * Guardian username (references users table).
     */
    @Column(name = "guardian_username",
            length = MAX_USERNAME_LENGTH,
            nullable = false)
    private String guardianUsername;

    /**
     * Announcement ID (references announcements table).
     */
    @Column(name = "announcement_id", nullable = false)
    private Integer announcementId;

    /**
     * Date when the favorite was added.
     */
    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded = LocalDateTime.now();
}
