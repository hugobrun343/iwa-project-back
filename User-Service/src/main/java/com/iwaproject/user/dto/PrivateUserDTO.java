package com.iwaproject.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for private user information (visible only to the user themselves).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateUserDTO {

    /**
     * Username.
     */
    private String username;

    /**
     * Email (from Keycloak).
     */
    private String email;

    /**
     * First name.
     */
    private String firstName;

    /**
     * Last name.
     */
    private String lastName;

    /**
     * Phone number.
     */
    private String phoneNumber;

    /**
     * Location.
     */
    private String location;

    /**
     * Description.
     */
    private String description;

    /**
     * Profile photo binary blob (base64-encoded in JSON).
     */
    private byte[] profilePhoto;

    /**
     * Identity verification status.
     */
    private Boolean identityVerification;

    /**
     * User preferences.
     */
    private String preferences;

    /**
     * Registration date.
     */
    private LocalDateTime registrationDate;
}
