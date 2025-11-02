package com.iwaproject.application.services;

import com.iwaproject.application.dtos.ApplicationRequestDto;
import com.iwaproject.application.dtos.ApplicationResponseDto;
import com.iwaproject.application.dtos.ApplicationUpdateStatusDto;
import com.iwaproject.application.entities.Application;
import com.iwaproject.application.entities.ApplicationStatus;
import com.iwaproject.application.repositories.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing applications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApplicationService {

    /**
     * Application repository.
     */
    private final ApplicationRepository applicationRepository;

    /**
     * Announcement owner Kafka service.
     */
    private final AnnouncementOwnerKafkaService announcementOwnerKafkaService;

    /**
     * Creates a new application.
     *
     * @param requestDto the application request data
     * @return the created application
     * @throws IllegalStateException if application already exists
     */
    public Application createApplication(
            final ApplicationRequestDto requestDto) {
        log.info("Creating application for announcement {} "
                + "by guardian {}",
                requestDto.getAnnouncementId(),
                requestDto.getGuardianUsername());

        // Check if the guardian is applying to their own announcement
        if (getAnnouncementOwnerUsername(
                requestDto.getAnnouncementId()).equals(
                        requestDto.getGuardianUsername())) {
            log.warn("Owner {} cannot apply to their own announcement {}",
                    requestDto.getGuardianUsername(),
                    requestDto.getAnnouncementId());
            throw new IllegalStateException(
                    "Owner cannot apply to their own announcement");
        }

        // Check if application already exists
        if (applicationRepository
                .existsByAnnouncementIdAndGuardianUsername(
                        requestDto.getAnnouncementId(),
                        requestDto.getGuardianUsername())) {
            log.warn("Application already exists for announcement {} "
                    + "and guardian {}", requestDto.getAnnouncementId(),
                    requestDto.getGuardianUsername());
            throw new IllegalStateException(
                    "Application already exists for this announcement "
                    + "and guardian");
        }

        Application application = new Application();
        application.setAnnouncementId(requestDto.getAnnouncementId());
        application.setGuardianUsername(requestDto.getGuardianUsername());
        application.setStatus(ApplicationStatus.SENT);
        application.setApplicationDate(LocalDateTime.now());

        Application savedApplication = applicationRepository.save(application);
        log.info("Application created with id {}", savedApplication.getId());

        return savedApplication;
    }

    /**
     * Creates a new application and returns the DTO.
     *
     * @param requestDto the application request data
     * @return the created application as DTO
     * @throws IllegalStateException if application already exists
     */
    public ApplicationResponseDto createApplicationDto(
            final ApplicationRequestDto requestDto) {
        Application application = createApplication(requestDto);
        return mapToResponseDto(application);
    }

    /**
     * Gets an application by its ID.
     *
     * @param id the application ID
     * @return the application
     * @throws IllegalArgumentException if application not found
     */
    @Transactional(readOnly = true)
    public ApplicationResponseDto getApplicationById(final Integer id) {
        log.info("Fetching application with id {}", id);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found with id: " + id));
        return mapToResponseDto(application);
    }

    /**
     * Gets all applications.
     *
     * @return list of all applications
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getAllApplications() {
        log.info("Fetching all applications");
        return applicationRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets applications by announcement ID.
     *
     * @param announcementId the announcement ID
     * @return list of applications
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByAnnouncementId(
            final Integer announcementId) {
        log.info("Fetching applications for announcement {}",
                announcementId);
        return applicationRepository.findByAnnouncementId(announcementId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets applications by guardian ID.
     *
     * @param guardianUsername the guardian username
     * @return list of applications
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByGuardianUsername(
            final String guardianUsername) {
        log.info("Fetching applications for guardian {}",
                guardianUsername);
        return applicationRepository
                .findByGuardianUsername(guardianUsername).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets applications by status.
     *
     * @param status the application status
     * @return list of applications
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByStatus(
            final ApplicationStatus status) {
        log.info("Fetching applications with status {}", status);
        return applicationRepository.findByStatus(status).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets applications by announcement ID and status.
     *
     * @param announcementId the announcement ID
     * @param status the application status
     * @return list of applications
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponseDto>
            getApplicationsByAnnouncementIdAndStatus(
            final Integer announcementId,
            final ApplicationStatus status) {
        log.info("Fetching applications for announcement {} with status {}",
                announcementId, status);
        return applicationRepository
                .findByAnnouncementIdAndStatus(announcementId, status)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets applications by guardian ID and status.
     *
     * @param guardianUsername the guardian username
     * @param status the application status
     * @return list of applications
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponseDto>
            getApplicationsByGuardianUsernameAndStatus(
                    final String guardianUsername,
                    final ApplicationStatus status) {
        log.info("Fetching applications for guardian {} "
                + "with status {}",
                guardianUsername, status);
        return applicationRepository
                .findByGuardianUsernameAndStatus(guardianUsername, status)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates the status of an application.
     *
     * @param id the application ID
     * @param updateDto the status update data
     * @return the updated application
     * @throws IllegalArgumentException if application not found
     */
    public ApplicationResponseDto updateApplicationStatus(final Integer id,
            final ApplicationUpdateStatusDto updateDto) {
        log.info("Updating status of application {} to {}", id,
                updateDto.getStatus());

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found with id: " + id));

        application.setStatus(updateDto.getStatus());
        Application updatedApplication =
                applicationRepository.save(application);

        log.info("Application {} status updated successfully", id);
        return mapToResponseDto(updatedApplication);
    }

    /**
     * Deletes an application.
     *
     * @param id the application ID
     * @throws IllegalArgumentException if application not found
     */
    public void deleteApplication(final Integer id) {
        log.info("Deleting application with id {}", id);

        if (!applicationRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Application not found with id: " + id);
        }

        applicationRepository.deleteById(id);
        log.info("Application {} deleted successfully", id);
    }

    private String getAnnouncementOwnerUsername(
            final Integer announcementId) {
        // Use Kafka to asynchronously get announcement owner
        try {
            return announcementOwnerKafkaService
                    .getAnnouncementOwner(announcementId)
                    .get(); // Blocks until response received or timeout
        } catch (Exception e) {
            System.err.println("⚠️ Error getting announcement owner: "
                    + e.getMessage());
            return null;
        }
    }

    private ApplicationResponseDto mapToResponseDto(
            final Application application) {
        return new ApplicationResponseDto(
                application.getId(),
                application.getAnnouncementId(),
                application.getGuardianUsername(),
                application.getStatus(),
                application.getApplicationDate()
        );
    }
}
