package com.iwaproject.favorite.services;

import com.iwaproject.favorite.dto.CreateFavoriteDTO;
import com.iwaproject.favorite.dto.FavoriteDTO;
import com.iwaproject.favorite.entities.Favorite;
import com.iwaproject.favorite.repositories.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for favorite operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    /**
     * Favorite repository.
     */
    private final FavoriteRepository favoriteRepository;

    /**
     * Get all favorites for a guardian.
     *
     * @param guardianUsername the guardian username
     * @return list of favorite DTOs
     */
    public List<FavoriteDTO> getFavoritesByGuardian(
            final String guardianUsername) {
        log.debug("Fetching favorites for guardian: {}", guardianUsername);
        return favoriteRepository.findByGuardianUsername(guardianUsername)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all favorites for an announcement.
     *
     * @param announcementId the announcement ID
     * @return list of favorite DTOs
     */
    public List<FavoriteDTO> getFavoritesByAnnouncement(
            final Integer announcementId) {
        log.debug("Fetching favorites for announcement: {}", announcementId);
        return favoriteRepository.findByAnnouncementId(announcementId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Add a favorite.
     *
     * @param guardianUsername the guardian username
     * @param createDTO        the create favorite DTO
     * @return created favorite DTO
     */
    @Transactional
    public FavoriteDTO addFavorite(final String guardianUsername,
                                    final CreateFavoriteDTO createDTO) {
        log.info("Adding favorite for guardian: {}, announcement: {}",
                guardianUsername, createDTO.getAnnouncementId());

        // Check if favorite already exists
        if (favoriteRepository.existsByGuardianUsernameAndAnnouncementId(
                guardianUsername, createDTO.getAnnouncementId())) {
            throw new IllegalStateException(
                    "Favorite already exists for this announcement");
        }

        Favorite favorite = new Favorite();
        favorite.setGuardianUsername(guardianUsername);
        favorite.setAnnouncementId(createDTO.getAnnouncementId());
        favorite.setDateAdded(LocalDateTime.now());

        Favorite saved = favoriteRepository.save(favorite);
        return mapToDTO(saved);
    }

    /**
     * Remove a favorite.
     *
     * @param guardianUsername the guardian username
     * @param announcementId   the announcement ID
     */
    @Transactional
    public void removeFavorite(final String guardianUsername,
                                final Integer announcementId) {
        log.info("Removing favorite for guardian: {}, announcement: {}",
                guardianUsername, announcementId);

        if (!favoriteRepository.existsByGuardianUsernameAndAnnouncementId(
                guardianUsername, announcementId)) {
            throw new IllegalArgumentException(
                    "Favorite not found for this announcement");
        }

        favoriteRepository.deleteByGuardianUsernameAndAnnouncementId(
                guardianUsername, announcementId);
    }

    /**
     * Check if a favorite exists.
     *
     * @param guardianUsername the guardian username
     * @param announcementId   the announcement ID
     * @return true if exists, false otherwise
     */
    public boolean isFavorite(final String guardianUsername,
                               final Integer announcementId) {
        log.debug("Checking if favorite exists for guardian: {}, "
                + "announcement: {}", guardianUsername, announcementId);
        return favoriteRepository.existsByGuardianUsernameAndAnnouncementId(
                guardianUsername, announcementId);
    }

    /**
     * Map Favorite entity to DTO.
     *
     * @param favorite the favorite entity
     * @return favorite DTO
     */
    private FavoriteDTO mapToDTO(final Favorite favorite) {
        FavoriteDTO dto = new FavoriteDTO();
        dto.setId(favorite.getId());
        dto.setGuardianUsername(favorite.getGuardianUsername());
        dto.setAnnouncementId(favorite.getAnnouncementId());
        dto.setDateAdded(favorite.getDateAdded());
        return dto;
    }
}
