package com.iwaproject.user.services;

import com.iwaproject.user.dto.UserLanguageDTO;
import com.iwaproject.user.dto.UserSpecialisationDTO;
import com.iwaproject.user.entities.User;
import com.iwaproject.user.entities.UserLanguage;
import com.iwaproject.user.entities.UserSpecialisation;
import com.iwaproject.user.keycloak.KeycloakClientService;
import com.iwaproject.user.repositories.LanguageRepository;
import com.iwaproject.user.repositories.SpecialisationRepository;
import com.iwaproject.user.repositories.UserLanguageRepository;
import com.iwaproject.user.repositories.UserRepository;
import com.iwaproject.user.repositories.UserSpecialisationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for user operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * User repository.
     */
    private final UserRepository userRepository;

    /**
     * User language repository.
     */
    private final UserLanguageRepository userLanguageRepository;

    /**
     * User specialisation repository.
     */
    private final UserSpecialisationRepository userSpecialisationRepository;

    /**
     * Language repository.
     */
    private final LanguageRepository languageRepository;

    /**
     * Specialisation repository.
     */
    private final SpecialisationRepository specialisationRepository;

    /**
     * Keycloak client service.
     */
    private final KeycloakClientService keycloakClientService;

    /**
     * Determine if a user's profile is complete based on required fields.
     *
     * Contract:
     * - Input: username (non-null)
     * - Output: true if all required fields are present/valid, else false
     * - Required fields (assumed): firstName, lastName, phoneNumber, location
     *   and at least one language and one specialisation.
     * - If user doesn't exist: returns false.
     *
     * @param username the username to evaluate
     * @return true if the profile is complete, otherwise false
     */
    public boolean isUserProfileComplete(final String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();

        boolean hasNames =
                (user.getFirstName() != null
                        && !user.getFirstName().isBlank())
                && (user.getLastName() != null
                        && !user.getLastName().isBlank());
        boolean hasPhone =
                user.getPhoneNumber() != null
                        && !user.getPhoneNumber().isBlank();
        boolean hasLocation =
                user.getLocation() != null
                        && !user.getLocation().isBlank();

        boolean hasLanguages = !userLanguageRepository
                .findByUsername(username).isEmpty();
        boolean hasSpecialisations = !userSpecialisationRepository
                .findByUsername(username).isEmpty();

        return hasNames && hasPhone && hasLocation
                && hasLanguages && hasSpecialisations;
    }

    /**
     * Get user by username.
     *
     * @param username the username
     * @return Optional containing user if found
     */
    public Optional<User> getUserByUsername(final String username) {
        log.debug("Fetching user: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Check if a user exists by username.
     *
     * Contract:
     * - Input: non-null username
     * - Output: true if a user row exists, false otherwise
     * - Errors: none, delegates to repository which returns boolean
     *
     * @param username the username to check
     * @return true if user exists, otherwise false
     */
    public boolean userExists(final String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        boolean exists = userRepository.existsByUsername(username);
        log.debug("User exists check for '{}': {}", username, exists);
        return exists;
    }

    /**
     * Get user email from Keycloak.
     *
     * @param username the username
     * @return user email or null if not found
     */
    public String getUserEmail(final String username) {
        try {
            return keycloakClientService.getEmailByUsername(username);
        } catch (Exception e) {
            log.warn("Could not fetch email from Keycloak for user: {}",
                    username, e);
            return null;
        }
    }

    /**
     * Update user profile.
     *
     * @param username the username
     * @param updates the updates to apply
     * @return updated user
     */
    @Transactional
    public User updateUserProfile(final String username,
            final Map<String, Object> updates) {
        log.info("Updating user profile for: {}", username);
        log.debug("Update payload: {}", updates);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found"));

        updates.forEach((key, value) -> {
            log.debug("Processing update field: {} = {}", key, value);
            switch (key) {
                case "firstName":
                    user.setFirstName((String) value);
                    log.debug("Updated firstName to: {}", value);
                    break;
                case "lastName":
                    user.setLastName((String) value);
                    log.debug("Updated lastName to: {}", value);
                    break;
                case "email":
                    String emailValue = (String) value;
                    log.info("Updating email for user {}: {} -> {}", 
                            username, user.getEmail(), emailValue);
                    user.setEmail(emailValue);
                    log.debug("Updated email to: {}", emailValue);
                    break;
                case "phoneNumber":
                    user.setPhoneNumber((String) value);
                    log.debug("Updated phoneNumber to: {}", value);
                    break;
                case "location":
                    user.setLocation((String) value);
                    log.debug("Updated location to: {}", value);
                    break;
                case "description":
                    user.setDescription((String) value);
                    log.debug("Updated description to: {}", value);
                    break;
                case "profilePhoto":
                    user.setProfilePhoto((String) value);
                    log.debug("Updated profilePhoto to: {}", value);
                    break;
                case "identityVerification":
                    user.setIdentityVerification((Boolean) value);
                    log.debug("Updated identityVerification to: {}", value);
                    break;
                case "preferences":
                    user.setPreferences((String) value);
                    log.debug("Updated preferences to: {}", value);
                    break;
                default:
                    log.warn("Unknown field for update: {}", key);
                    break;
            }
        });
        User savedUser = userRepository.save(user);
        log.info("User profile updated successfully. Email after save: {}", 
                savedUser.getEmail());
        return savedUser;
    }

    /**
     * Create user profile.
     *
     * @param username the username
     * @param firstName the first name
     * @param lastName the last name
     * @return created user
     */
    @Transactional
    public User createUserProfile(final String username,
            final String firstName, final String lastName) {
        log.info("Creating user profile for: {}", username);
        User user = new User();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return userRepository.save(user);
    }

    /**
     * Create user profile from a payload map.
     *
     * @param username the username
     * @param payload  payload with optional fields
     * @return created user
     */
    @Transactional
    public User createUserProfile(final String username,
            final Map<String, Object> payload) {
        log.info("Creating user profile (map) for: {}", username);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("User already exists");
        }

        User user = new User();
        user.setUsername(username);

        Object firstName = payload.get("firstName");
        Object lastName = payload.get("lastName");
        if (firstName == null || lastName == null) {
            throw new IllegalArgumentException(
                    "firstName and lastName are required");
        }
        user.setFirstName((String) firstName);
        user.setLastName((String) lastName);

        // Optional fields
        if (payload.containsKey("email")) {
            user.setEmail((String) payload.get("email"));
        }
        if (payload.containsKey("phoneNumber")) {
            user.setPhoneNumber((String) payload.get("phoneNumber"));
        }
        if (payload.containsKey("location")) {
            user.setLocation((String) payload.get("location"));
        }
        if (payload.containsKey("description")) {
            user.setDescription((String) payload.get("description"));
        }
        if (payload.containsKey("profilePhoto")) {
            user.setProfilePhoto((String) payload.get("profilePhoto"));
        }
        if (payload.containsKey("identityVerification")) {
            Object iv = payload.get("identityVerification");
            if (iv instanceof Boolean) {
                user.setIdentityVerification((Boolean) iv);
            }
        }
        if (payload.containsKey("preferences")) {
            user.setPreferences((String) payload.get("preferences"));
        }

        return userRepository.save(user);
    }

    /**
     * Get user's chosen languages.
     *
     * @param username the username
     * @return list of user languages
     */
    public List<UserLanguageDTO> getUserLanguages(final String username) {
        log.debug("Fetching languages for user: {}", username);
        return userLanguageRepository.findByUsername(username).stream()
                .map(ul -> UserLanguageDTO.fromLanguage(
                        ul.getLanguage().getLabel()))
                .collect(Collectors.toList());
    }

    /**
     * Update user's chosen languages.
     *
     * @param username the username
     * @param languageLabels the language labels
     * @return list of updated user languages
     */
    @Transactional
    public List<UserLanguageDTO> updateUserLanguages(final String username,
            final List<String> languageLabels) {
        log.info(
                "Updating languages for user: {} with languages: {}",
                username, languageLabels);
        userLanguageRepository.deleteByUsername(username);

        List<UserLanguage> newLanguages = languageLabels.stream()
                .map(label -> languageRepository.findById(label)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Language not found: " + label)))
                .map(language -> {
                    UserLanguage ul = new UserLanguage();
                    ul.setUsername(username);
                    ul.setLanguage(language);
                    return ul;
                })
                .collect(Collectors.toList());

        userLanguageRepository.saveAll(newLanguages);
        return newLanguages.stream()
                .map(ul -> UserLanguageDTO.fromLanguage(
                        ul.getLanguage().getLabel()))
                .collect(Collectors.toList());
    }

    /**
     * Get user's chosen specialisations.
     *
     * @param username the username
     * @return list of user specialisations
     */
    public List<UserSpecialisationDTO> getUserSpecialisations(
            final String username) {
        log.debug("Fetching specialisations for user: {}", username);
        return userSpecialisationRepository.findByUsername(username).stream()
                .map(us -> UserSpecialisationDTO.fromSpecialisation(
                        us.getSpecialisation().getLabel()))
                .collect(Collectors.toList());
    }

    /**
     * Update user's chosen specialisations.
     *
     * @param username the username
     * @param specialisationLabels the specialisation labels
     * @return list of updated user specialisations
     */
    @Transactional
    public List<UserSpecialisationDTO> updateUserSpecialisations(
            final String username, final List<String> specialisationLabels) {
        log.info("Updating specialisations for user: {}", username);
        userSpecialisationRepository.deleteByUsername(username);

        List<UserSpecialisation> newSpecialisations =
                specialisationLabels.stream()
                .map(label -> specialisationRepository.findById(label)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Specialisation not found: " + label)))
                .map(specialisation -> {
                    UserSpecialisation us = new UserSpecialisation();
                    us.setUsername(username);
                    us.setSpecialisation(specialisation);
                    return us;
                })
                .collect(Collectors.toList());

        userSpecialisationRepository.saveAll(newSpecialisations);
        return newSpecialisations.stream()
                .map(us -> UserSpecialisationDTO.fromSpecialisation(
                        us.getSpecialisation().getLabel()))
                .collect(Collectors.toList());
    }
}
