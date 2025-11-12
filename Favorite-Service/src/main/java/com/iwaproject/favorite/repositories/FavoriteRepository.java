package com.iwaproject.favorite.repositories;

import com.iwaproject.favorite.entities.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Favorite entity.
 */
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {

    /**
     * Find all favorites by guardian username.
     *
     * @param guardianUsername the guardian username
     * @return list of favorites
     */
    List<Favorite> findByGuardianUsername(String guardianUsername);

    /**
     * Find a specific favorite by guardian username and announcement ID.
     *
     * @param guardianUsername the guardian username
     * @param announcementId   the announcement ID
     * @return optional favorite
     */
    Optional<Favorite> findByGuardianUsernameAndAnnouncementId(
            String guardianUsername, Integer announcementId);

    /**
     * Delete a favorite by guardian username and announcement ID.
     *
     * @param guardianUsername the guardian username
     * @param announcementId   the announcement ID
     */
    void deleteByGuardianUsernameAndAnnouncementId(
            String guardianUsername, Integer announcementId);

    /**
     * Check if a favorite exists for a guardian and announcement.
     *
     * @param guardianUsername the guardian username
     * @param announcementId   the announcement ID
     * @return true if exists, false otherwise
     */
    boolean existsByGuardianUsernameAndAnnouncementId(
            String guardianUsername, Integer announcementId);

    /**
     * Find all favorites by announcement ID.
     *
     * @param announcementId the announcement ID
     * @return list of favorites
     */
    List<Favorite> findByAnnouncementId(Integer announcementId);
}
