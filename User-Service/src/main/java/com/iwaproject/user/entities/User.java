package com.iwaproject.user.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User entity - stores user profile data.
 * Keycloak handles only authentication (username/password/email).
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Maximum length for standard text fields.
     */
    private static final int MAX_TEXT_LENGTH = 255;

    /**
     * Maximum length for phone number.
     */
    private static final int MAX_PHONE_LENGTH = 15;

    /**
     * Maximum length for description.
     */
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    /**
     * Maximum length for preferences.
     */
    private static final int MAX_PREFERENCES_LENGTH = 2000;

    /**
     * Username (primary key, matches Keycloak username).
     */
    @Id
    @Column(name = "username", length = MAX_TEXT_LENGTH, nullable = false)
    private String username;

    /**
     * User's first name.
     */
    @Column(name = "first_name", length = MAX_TEXT_LENGTH, nullable = false)
    private String firstName;

    /**
     * User's last name.
     */
    @Column(name = "last_name", length = MAX_TEXT_LENGTH, nullable = false)
    private String lastName;

    /**
     * User's email.
     */
    @Column(name = "email", length = MAX_TEXT_LENGTH)
    private String email;

    /**
     * User's phone number.
     */
    @Column(name = "phone_number", length = MAX_PHONE_LENGTH)
    private String phoneNumber;

    /**
     * User's location.
     */
    @Column(name = "location", length = MAX_TEXT_LENGTH)
    private String location;

    /**
     * User's personal description.
     */
    @Column(name = "description", length = MAX_DESCRIPTION_LENGTH)
    private String description;

    /**
     * User's profile photo binary data.
     */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "profile_photo", columnDefinition = "BYTEA")
    private byte[] profilePhoto;

    /**
     * Identity verification status.
     */
    @Column(name = "identity_verification")
    private Boolean identityVerification = false;

    /**
     * User preferences (JSON string).
     */
    @Column(name = "preferences", length = MAX_PREFERENCES_LENGTH)
    private String preferences;

    /**
     * Registration date.
     */
    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate = LocalDateTime.now();
}
