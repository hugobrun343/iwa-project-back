package com.iwaproject.application.controllers;

import com.iwaproject.application.dtos.ApplicationRequestDto;
import com.iwaproject.application.dtos.ApplicationResponseDto;
import com.iwaproject.application.dtos.ApplicationUpdateStatusDto;
import com.iwaproject.application.entities.ApplicationStatus;
import com.iwaproject.application.services.ApplicationService;
import com.iwaproject.application.services.KafkaLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing applications (candidatures).
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
public final class ApplicationController {

    /**
     * Application service.
     */
    private final ApplicationService applicationService;

    /**
     * Kafka log service.
     */
    private final KafkaLogService kafkaLogService;

    /**
     * Logger name constant.
     */
    private static final String LOGGER_NAME = "ApplicationController";

    /**
     * Creates a new application.
     *
     * @param requestDto the application request data
     * @return the created application response
     */
    @PostMapping
    public ResponseEntity<ApplicationResponseDto> createApplication(
            final @RequestBody ApplicationRequestDto requestDto) {
        kafkaLogService.info(LOGGER_NAME,
                "POST /api/applications"
                + ", Creating new application");
        try {
            ApplicationResponseDto response =
                    applicationService.createApplicationDto(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Application already exists: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Error creating application: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Gets an application by its ID.
     *
     * @param id the application ID
     * @return the application response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> getApplicationById(
            final @PathVariable Integer id) {
        kafkaLogService.info(LOGGER_NAME,
                "GET /api/applications/" + id
                + ", Fetching application");
        try {
            ApplicationResponseDto response =
                    applicationService.getApplicationById(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Application not found with id " + id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Error fetching application: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Gets all applications with optional filters.
     *
     * @param announcementId optional announcement ID filter
     * @param guardianUsername optional guardian username filter
     * @param status optional status filter
     * @return list of applications
     */
    @GetMapping
    public ResponseEntity<List<ApplicationResponseDto>> getAllApplications(
            final @RequestParam(required = false) Integer announcementId,
            final @RequestParam(required = false) String guardianUsername,
            final @RequestParam(required = false) ApplicationStatus status) {
        kafkaLogService.info(LOGGER_NAME,
                "GET /api/applications"
                + ", Fetching applications with filters");
        try {
            List<ApplicationResponseDto> responses;

            if (announcementId != null && status != null) {
                responses = applicationService
                        .getApplicationsByAnnouncementIdAndStatus(
                                announcementId, status);
            } else if (guardianUsername != null && status != null) {
                responses = applicationService
                        .getApplicationsByGuardianUsernameAndStatus(
                                guardianUsername, status);
            } else if (announcementId != null) {
                responses = applicationService
                        .getApplicationsByAnnouncementId(announcementId);
            } else if (guardianUsername != null) {
                responses = applicationService
                        .getApplicationsByGuardianUsername(guardianUsername);
            } else if (status != null) {
                responses = applicationService.getApplicationsByStatus(status);
            } else {
                responses = applicationService.getAllApplications();
            }

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Error fetching applications: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Updates the status of an application.
     *
     * @param id the application ID
     * @param updateDto the status update data
     * @return the updated application response
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponseDto> updateApplicationStatus(
            final @PathVariable Integer id,
            final @RequestBody ApplicationUpdateStatusDto updateDto) {
        kafkaLogService.info(LOGGER_NAME,
                "PATCH /api/applications/" + id + "/status"
                + ", Updating status");
        try {
            ApplicationResponseDto response =
                    applicationService.updateApplicationStatus(id, updateDto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Application not found with id " + id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Error updating application status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Deletes an application.
     *
     * @param id the application ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            final @PathVariable Integer id) {
        kafkaLogService.info(LOGGER_NAME,
                "DELETE /api/applications/" + id
                + ", Deleting application");
        try {
            applicationService.deleteApplication(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Application not found with id " + id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            kafkaLogService.error(LOGGER_NAME,
                    "Error deleting application: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
