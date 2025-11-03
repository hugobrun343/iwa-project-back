package com.iwaproject.chat.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Discussion entity - represents a conversation between two users about an announcement.
 * Each discussion is linked to two members (sender and recipient) and one announcement.
 * Multiple discussions can exist between the same two people, but each discussion is tied to a specific announcement.
 */
@Entity
@Table(name = "discussions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Discussion {

    /**
     * Maximum length for standard text fields.
     */
    private static final int MAX_TEXT_LENGTH = 255;

    /**
     * Discussion ID (primary key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Announcement ID (required - each discussion is linked to an announcement).
     */
    @Column(name = "annonce_id", nullable = false)
    private Long announcementId;

    /**
     * Sender ID (user who initiated the discussion).
     */
    @Column(name = "expediteur_id", length = MAX_TEXT_LENGTH, nullable = false)
    private String senderId;

    /**
     * Recipient ID (user who receives the discussion).
     */
    @Column(name = "destinataire_id", length = MAX_TEXT_LENGTH, nullable = false)
    private String recipientId;

    /**
     * Creation date.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Last update date.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Messages in this discussion.
     */
    @OneToMany(mappedBy = "discussion", fetch = FetchType.LAZY)
    private List<Message> messages;
}

