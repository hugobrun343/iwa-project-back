package com.iwaproject.favorite.controllers;

import com.iwaproject.favorite.dto.CreateFavoriteDTO;
import com.iwaproject.favorite.dto.FavoriteDTO;
import com.iwaproject.favorite.services.FavoriteService;
import com.iwaproject.favorite.services.KafkaLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Controller for favorite operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    /**
     * Favorite service.
     */
    private final FavoriteService favoriteService;

    /**
     * Kafka log service.
     */
    private final KafkaLogService kafkaLogService;

    /**
     * Logger name constant.
     */
    private static final String LOGGER_NAME = "FavoriteController";

    /**
     * HTTP status code for conflict.
     */
    private static final int HTTP_STATUS_CONFLICT = 409;

    /**
     * Get all favorites for the current guardian.
     *
     * @param username the username from header
     * @return list of favorites
     */
    @GetMapping
    public ResponseEntity<List<FavoriteDTO>> getMyFavorites(
            @RequestHeader("X-Username") final String username) {

        kafkaLogService.info(LOGGER_NAME,
                "GET /api/favorites - User: " + username);

        try {
            List<FavoriteDTO> favorites =
                    favoriteService.getFavoritesByGuardian(username);
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to get favorites: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if an announcement is favorited by the current guardian.
     *
     * @param username the username from header
     * @param announcementId the announcement ID
     * @return true if favorited, false otherwise
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @RequestHeader("X-Username") final String username,
            @RequestParam final Integer announcementId) {

        kafkaLogService.info(LOGGER_NAME,
                "GET /api/favorites/check - User: " + username
                        + ", Announcement: " + announcementId);

        try {
            boolean isFavorite =
                    favoriteService.isFavorite(username, announcementId);
            return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to check favorite: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Add a new favorite.
     *
     * @param username the username from header
     * @param createDTO the create favorite DTO
     * @return created favorite
     */
    @PostMapping
    public ResponseEntity<FavoriteDTO> addFavorite(
            @RequestHeader("X-Username") final String username,
            @Valid @RequestBody final CreateFavoriteDTO createDTO) {

        kafkaLogService.info(LOGGER_NAME,
                "POST /api/favorites - User: " + username
                        + ", Announcement: " + createDTO.getAnnouncementId());

        try {
            FavoriteDTO favorite =
                    favoriteService.addFavorite(username, createDTO);
            return ResponseEntity.created(
                            URI.create("/api/favorites/" + favorite.getId()))
                    .body(favorite);
        } catch (IllegalStateException e) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Favorite already exists: " + e.getMessage());
            return ResponseEntity.status(HTTP_STATUS_CONFLICT).build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to add favorite: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Remove a favorite.
     *
     * @param username the username from header
     * @param announcementId the announcement ID
     * @return no content response
     */
    @DeleteMapping("/{announcementId}")
    public ResponseEntity<Void> removeFavorite(
            @RequestHeader("X-Username") final String username,
            @PathVariable final Integer announcementId) {

        kafkaLogService.info(LOGGER_NAME,
                "DELETE /api/favorites/" + announcementId
                        + " - User: " + username);

        try {
            favoriteService.removeFavorite(username, announcementId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            kafkaLogService.warn(LOGGER_NAME,
                    "Favorite not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Failed to remove favorite: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
