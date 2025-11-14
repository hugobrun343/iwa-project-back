package com.iwaproject.user.controllers;

import com.iwaproject.user.dto.LanguageDTO;
import com.iwaproject.user.dto.PrivateUserDTO;
import com.iwaproject.user.dto.PublicUserDTO;
import com.iwaproject.user.dto.SpecialisationDTO;
import com.iwaproject.user.dto.UserLanguageDTO;
import com.iwaproject.user.dto.UserSpecialisationDTO;
import com.iwaproject.user.dto.UserProfileCompletionDTO;
import com.iwaproject.user.entities.Language;
import com.iwaproject.user.entities.Specialisation;
import com.iwaproject.user.entities.User;
import com.iwaproject.user.services.KafkaLogService;
import com.iwaproject.user.services.LanguageService;
import com.iwaproject.user.services.SpecialisationService;
import com.iwaproject.user.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.iwaproject.user.dto.UserExistenceDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.net.URI;

/**
 * Main controller for user operations.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    /**
     * User service.
     */
    private final UserService userService;

    /**
     * Language service.
     */
    private final LanguageService languageService;

    /**
     * Specialisation service.
     */
    private final SpecialisationService specialisationService;

    /**
     * Kafka log service.
     */
    private final KafkaLogService kafkaLogService;

    /**
     * Logger name constant.
     */
    private static final String LOGGER_NAME = "UserController";

    /**
     * HTTP status code for conflict.
     */
    private static final int HTTP_STATUS_CONFLICT = 409;

    /**
     * Create a new user profile.
     *
     * @param username the username (from gateway/keycloak)
     * @param emailHeader optional email propagated by gateway
     * @param payload  the request body containing profile fields
     * @return created user profile
     */
    @PostMapping("/users")
    public ResponseEntity<PrivateUserDTO> createUser(
        @RequestHeader("X-Username") final String username,
            @RequestBody final Map<String, Object> payload) {

        kafkaLogService.info(LOGGER_NAME,
                "POST /users - User: " + username);

        try {
            // Basic validation
            if (!payload.containsKey("firstName")
                    || !payload.containsKey("lastName")) {
                kafkaLogService.warn(LOGGER_NAME,
                        "Missing required fields: firstName or lastName");
                return ResponseEntity.badRequest().build();
            }

            User created = userService.createUserProfile(username, payload);
            PrivateUserDTO dto = mapToPrivateUserDTO(created);

            return ResponseEntity.created(
                            URI.create("/api/users/" + username))
                    .body(dto);
        } catch (IllegalStateException e) {
            kafkaLogService.warn(LOGGER_NAME,
                    "User already exists: " + username);
            return ResponseEntity.status(HTTP_STATUS_CONFLICT).build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to create user: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check if a user exists by username.
     *
     * @param username the username to check
     * @return true if user exists, else false
     */
    @GetMapping("/users/exists")
    public ResponseEntity<UserExistenceDTO> userExists(
            @RequestParam("username") final String username) {

        kafkaLogService.info(LOGGER_NAME,
                "GET /users/exists - username=" + username);

        boolean exists = userService.userExists(username);
        UserExistenceDTO dto = new UserExistenceDTO(username, exists);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get current user profile.
     *
     * @param usernameHeader username from header (optional)
     * @param emailHeader optional email propagated by gateway
     * @param usernameParam username as query parameter (optional)
     * @return user profile
     */
    @GetMapping("/users/me")
    public ResponseEntity<PrivateUserDTO> getMyProfile(
        @RequestHeader(value = "X-Username", required = false)
            final String usernameHeader,
        @RequestParam(value = "username", required = false)
            final String usernameParam) {
        String username = (usernameParam != null && !usernameParam.isEmpty())
                ? usernameParam : usernameHeader;
        if (username == null || username.isEmpty()) {
            kafkaLogService.warn(
                    LOGGER_NAME,
                    "GET /users/me - missing username"
                            + "(no query param and no header)");
            return ResponseEntity.badRequest().build();
        }

        kafkaLogService.info(LOGGER_NAME, "GET /users/me - User: " + username);

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            kafkaLogService.warn(LOGGER_NAME, "User not found: " + username);
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        PrivateUserDTO dto = mapToPrivateUserDTO(user);

        kafkaLogService.debug(
            LOGGER_NAME,
            "user data: username=" + dto.getUsername()
                + ", email=" + dto.getEmail()
                + ", firstName=" + dto.getFirstName()
                + ", lastName=" + dto.getLastName()
                + ", phoneNumber=" + dto.getPhoneNumber()
                + ", location=" + dto.getLocation()
                + ", description=" + dto.getDescription()
                + ", profilePhoto=" + dto.getProfilePhoto()
                + ", identityVerification=" + dto.getIdentityVerification()
                + ", preferences=" + dto.getPreferences()
                + ", registrationDate=" + dto.getRegistrationDate());
        return ResponseEntity.ok(dto);
    }

    /**
     * Get public user profile.
     *
     * @param username the username
     * @return public user profile
     */
    @GetMapping("/users/{username}")
    public ResponseEntity<PublicUserDTO> getUserProfile(
            @PathVariable final String username) {

        kafkaLogService.info(LOGGER_NAME, "GET /users/" + username);

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            kafkaLogService.warn(LOGGER_NAME, "User not found: " + username);
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        PublicUserDTO dto = mapToPublicUserDTO(user);
        return ResponseEntity.ok(dto);
    }

    /**
     * Update user profile.
     *
     * @param username the username
     * @param emailHeader optional email propagated by gateway
     * @param updates the updates to apply
     * @return updated user profile
     */
    @PatchMapping("/users/me")
    public ResponseEntity<PrivateUserDTO> updateMyProfile(
        @RequestHeader("X-Username") final String username,
            @RequestBody final Map<String, Object> updates) {

        kafkaLogService.info(LOGGER_NAME,
                "PATCH /users/me - User: " + username);

        try {
            User updatedUser = userService.updateUserProfile(username, updates);
            PrivateUserDTO dto = mapToPrivateUserDTO(updatedUser);
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to update profile: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all available languages.
     *
     * @return list of languages
     */
    @GetMapping("/languages")
    public ResponseEntity<List<LanguageDTO>> getLanguages() {
        kafkaLogService.info(LOGGER_NAME, "GET /languages");

        List<Language> languages = languageService.getAllLanguages();
        List<LanguageDTO> dtos = languages.stream()
                .map(lang -> new LanguageDTO(lang.getLabel()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all available specialisations.
     *
     * @return list of specialisations
     */
    @GetMapping("/specialisations")
    public ResponseEntity<List<SpecialisationDTO>> getSpecialisations() {
        kafkaLogService.info(LOGGER_NAME, "GET /specialisations");

        List<Specialisation> specialisations =
                specialisationService.getAllSpecialisations();
        List<SpecialisationDTO> dtos = specialisations.stream()
                .map(spec -> new SpecialisationDTO(spec.getLabel()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get user's chosen languages.
     *
     * @param username the username
     * @return list of user languages
     */
    @GetMapping("/users/me/languages")
    public ResponseEntity<List<UserLanguageDTO>> getMyLanguages(
            @RequestHeader("X-Username") final String username) {

        kafkaLogService.info(LOGGER_NAME,
                "GET /users/me/languages - User: " + username);

        List<UserLanguageDTO> languages =
                userService.getUserLanguages(username);
        return ResponseEntity.ok(languages);
    }

    /**
     * Update user's chosen languages.
     *
     * @param username the username
     * @param request the request containing languages
     * @return updated user languages
     */
    @PatchMapping("/users/me/languages")
    public ResponseEntity<List<UserLanguageDTO>> updateMyLanguages(
            @RequestHeader("X-Username") final String username,
            @RequestBody final Map<String, List<String>> request) {

        kafkaLogService.info(LOGGER_NAME,
                "PATCH /users/me/languages - User: " + username);

        try {
            List<String> languageLabels = request.get("languages");
            if (languageLabels == null) {
                kafkaLogService.warn(LOGGER_NAME,
                        "Missing 'languages' field in request");
                return ResponseEntity.badRequest().build();
            }

            List<UserLanguageDTO> updatedLanguages =
                    userService.updateUserLanguages(username, languageLabels);
            return ResponseEntity.ok(updatedLanguages);
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to update languages: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user's chosen specialisations.
     *
     * @param username the username
     * @return list of user specialisations
     */
    @GetMapping("/users/me/specialisations")
    public ResponseEntity<List<UserSpecialisationDTO>> getMySpecialisations(
            @RequestHeader("X-Username") final String username) {

        kafkaLogService.info(LOGGER_NAME,
                "GET /users/me/specialisations - User: " + username);

        List<UserSpecialisationDTO> specialisations =
                userService.getUserSpecialisations(username);
        return ResponseEntity.ok(specialisations);
    }

    /**
     * Update user's chosen specialisations.
     *
     * @param username the username
     * @param request the request containing specialisations
     * @return updated user specialisations
     */
    @PatchMapping("/users/me/specialisations")
    public ResponseEntity<List<UserSpecialisationDTO>> updateMySpecialisations(
            @RequestHeader("X-Username") final String username,
            @RequestBody final Map<String, List<String>> request) {

        kafkaLogService.info(LOGGER_NAME,
                "PATCH /users/me/specialisations - User: " + username);

        try {
            List<String> specialisationLabels = request.get("specialisations");
            if (specialisationLabels == null) {
                kafkaLogService.warn(LOGGER_NAME,
                        "Missing 'specialisations' field in request");
                return ResponseEntity.badRequest().build();
            }

            List<UserSpecialisationDTO> updatedSpecialisations =
                    userService.updateUserSpecialisations(username,
                            specialisationLabels);
            return ResponseEntity.ok(updatedSpecialisations);
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to update specialisations: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check whether a given user's profile is complete.
     *
     * @param username the username to check
     * @return UserProfileCompletionDTO containing username and completion flag
     */
    @GetMapping("/users/{username}/profile-complete")
    public ResponseEntity<UserProfileCompletionDTO> isUserProfileComplete(
            @PathVariable("username") final String username) {
        kafkaLogService.info(LOGGER_NAME,
                "GET /users/" + username + "/profile-complete");

        if (userService.getUserByUsername(username).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean complete = userService.isUserProfileComplete(username);
        return ResponseEntity.ok(
                new UserProfileCompletionDTO(username, complete));
    }

    /**
     * Map User entity to PrivateUserDTO.
     *
     * @param user the user entity
     * @return private user DTO
     */
    private PrivateUserDTO mapToPrivateUserDTO(final User user) {
        PrivateUserDTO dto = new PrivateUserDTO();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setLocation(user.getLocation());
        dto.setDescription(user.getDescription());
        dto.setProfilePhoto(user.getProfilePhoto());
        dto.setIdentityVerification(user.getIdentityVerification());
        dto.setPreferences(user.getPreferences());
        dto.setRegistrationDate(user.getRegistrationDate());
        return dto;
    }

    /**
     * Map User entity to PublicUserDTO.
     *
     * @param user the user entity
     * @return public user DTO
     */
    private PublicUserDTO mapToPublicUserDTO(final User user) {
        PublicUserDTO dto = new PublicUserDTO();
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setLocation(user.getLocation());
        dto.setDescription(user.getDescription());
        dto.setProfilePhoto(user.getProfilePhoto());
        dto.setIdentityVerification(user.getIdentityVerification());
        dto.setRegistrationDate(user.getRegistrationDate());
        return dto;
    }
}
