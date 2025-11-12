package com.iwaproject.announcement.repositories;

import com.iwaproject.announcement.entities.Announcement;
import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Announcement entities.
 */
@Repository
public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {
    /**
     * Find announcements by owner username.
     *
     * @param ownerUsername the owner username
     * @return list of announcements
     */
    List<Announcement> findByOwnerUsername(String ownerUsername);

    /**
     * Find announcements by status.
     *
     * @param status the status
     * @return list of announcements
     */
    List<Announcement> findByStatus(AnnouncementStatus status);

    /**
     * Find announcements by owner username and status.
     *
     * @param ownerUsername the owner username
     * @param status the status
     * @return list of announcements
     */
    List<Announcement> findByOwnerUsernameAndStatus(String ownerUsername,
                                               AnnouncementStatus status);
}
