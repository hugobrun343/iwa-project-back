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
     * Creates a new application.
     *
     * @param requestDto the application request data
     * @return the created application
     * @throws IllegalStateException if application already exists
     */
    public ApplicationResponseDto createApplication(
            final ApplicationRequestDto requestDto) {
        log.info("Creating application for announcement {} by guardian {}",
                requestDto.getAnnouncementId(), requestDto.getGuardianId());

        // Check if application already exists
        if (applicationRepository.existsByAnnouncementIdAndGuardianId(
                requestDto.getAnnouncementId(), requestDto.getGuardianId())) {
            log.warn("Application already exists for announcement {} "
                    + "and guardian {}", requestDto.getAnnouncementId(),
                    requestDto.getGuardianId());
            throw new IllegalStateException(
                    "Application already exists for this announcement "
                    + "and guardian");
        }

        Application Application = new Application();
        Application.setAnnouncementId(requestDto.getAnnouncementId());
        Application.setGuardianId(requestDto.getGuardianId());
        Application.setStatus(ApplicationStatus.SENT);
        Application.setApplicationDate(LocalDateTime.now());

        Application savedApplication = applicationRepository.save(Application);
        log.info("Application created with id {}", savedApplication.getId());

        return mapToResponseDto(savedApplication);
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
        Application Application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found with id: " + id));
        return mapToResponseDto(Application);
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
     * @param guardianId the guardian ID
     * @return list of applications
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByGuardianId(
            final Integer guardianId) {
        log.info("Fetching applications for guardian {}", guardianId);
        return applicationRepository.findByGuardianId(guardianId).stream()
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
     * @param guardianId the guardian ID
     * @param status the application status
     * @return list of applications
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByGuardianIdAndStatus(
            final Integer guardianId, final ApplicationStatus status) {
        log.info("Fetching applications for guardian {} with status {}",
                guardianId, status);
        return applicationRepository
                .findByGuardianIdAndStatus(guardianId, status)
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

        Application Application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found with id: " + id));

        Application.setStatus(updateDto.getStatus());
        Application updatedApplication =
                applicationRepository.save(Application);

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

    private ApplicationResponseDto mapToResponseDto(
            final Application application) {
        return new ApplicationResponseDto(
                application.getId(),
                application.getAnnouncementId(),
                application.getGuardianId(),
                application.getStatus(),
                application.getApplicationDate()
        );
    }
}
