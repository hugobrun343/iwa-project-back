package com.iwaproject.rating.entities;

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
 * Rating entity - represents a rating/review from one user to another.
 * Each rating links an author (rater) to a recipient (rated user).
 */
@Entity
@Table(name = "avis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    /**
     * Maximum length for standard text fields.
     */
    private static final int MAX_TEXT_LENGTH = 255;

    /**
     * Rating ID (primary key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Author ID (user who gives the rating).
     */
    @Column(name = "auteur_id", length = MAX_TEXT_LENGTH, nullable = false)
    private String authorId;

    /**
     * Recipient ID (user who receives the rating).
     */
    @Column(name = "destinataire_id", length = MAX_TEXT_LENGTH, nullable = false)
    private String recipientId;

    /**
     * Rating score (typically 1-5).
     */
    @Column(name = "note", nullable = false)
    private Integer note;

    /**
     * Rating comment.
     */
    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    /**
     * Rating date.
     */
    @Column(name = "date_avis", nullable = false)
    private LocalDateTime dateAvis = LocalDateTime.now();
}

