package com.iwaproject.application.repositories;

import com.iwaproject.application.entities.Application;
import com.iwaproject.application.entities.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing Candidature entities.
 */
@Repository
public interface ApplicationRepository
        extends JpaRepository<Application, Integer> {

    /**
     * Finds all applications for an announcement.
     *
     * @param announcementId the announcement ID
     * @return list of applications
     */
    List<Application> findByAnnouncementId(Integer announcementId);

    /**
     * Finds all applications for a guardian.
     *
     * @param guardianUsername the guardian's username
     * @return list of applications
     */
    List<Application> findByGuardianUsername(String guardianUsername);

    /**
     * Finds all applications with a specific status.
     *
     * @param status the application status
     * @return list of applications
     */
    List<Application> findByStatus(ApplicationStatus status);

    /**
     * Finds applications by announcement and status.
     *
     * @param announcementId the announcement ID
     * @param status the application status
     * @return list of applications
     */
    List<Application> findByAnnouncementIdAndStatus(
            Integer announcementId, ApplicationStatus status);

    /**
     * Finds applications by guardian and status.
     *
     * @param guardianUsername the guardian's username
     * @param status the application status
     * @return list of applications
     */
    List<Application> findByGuardianUsernameAndStatus(
            String guardianUsername, ApplicationStatus status);

    /**
     * Checks if an application exists for announcement and guardian.
     *
     * @param announcementId the announcement ID
     * @param guardianUsername the guardian's username
     * @return true if exists, false otherwise
     */
    boolean existsByAnnouncementIdAndGuardianUsername(
            Integer announcementId, String guardianUsername);
}
