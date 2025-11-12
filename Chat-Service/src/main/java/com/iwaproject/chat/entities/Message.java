package com.iwaproject.chat.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message entity - represents a message in a discussion.
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /**
     * Maximum length for message content.
     */
    private static final int MAX_CONTENT_LENGTH = 5000;

    /**
     * Maximum length for user ID.
     */
    private static final int MAX_USER_ID_LENGTH = 255;

    /**
     * Message ID (primary key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Discussion this message belongs to.
     */
    @ManyToOne
    @JoinColumn(name = "discussion_id", nullable = false)
    private Discussion discussion;

    /**
     * Author ID (user who sent the message).
     */
    @Column(name = "auteur_id", length = MAX_USER_ID_LENGTH, nullable = false)
    private String authorId;

    /**
     * Message content.
     */
    @Column(name = "contenu", length = MAX_CONTENT_LENGTH, nullable = false)
    private String content;

    /**
     * Creation date.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

