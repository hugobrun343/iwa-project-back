package com.iwaproject.announcement.controllers;

import com.iwaproject.announcement.dto.AnnouncementMapper;
import com.iwaproject.announcement.dto.AnnouncementRequestDto;
import com.iwaproject.announcement.dto.AnnouncementResponseDto;
import com.iwaproject.announcement.entities.Announcement;
import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import com.iwaproject.announcement.services.AnnouncementService;
import com.iwaproject.announcement.services.KafkaLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing announcements.
 */
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    /**
     * The announcement service.
     */
    private final AnnouncementService announcementService;
    /**
     * The announcement mapper.
     */
    private final AnnouncementMapper announcementMapper;
    /**
     * The Kafka log service.
     */
    private final KafkaLogService kafkaLogService;

    /**
     * Create a new announcement.
     * POST /api/announcements
     *
     * @param username the username from the authentication header
     * @param requestDto the announcement request DTO
     * @return the created announcement with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<AnnouncementResponseDto> createAnnouncement(
            @RequestHeader("X-Username") final String username,
            @RequestBody final AnnouncementRequestDto requestDto) {
        kafkaLogService.info("AnnouncementController",
                "=== CREATE ANNOUNCEMENT REQUEST === User: " + username
                        + ", Title: " + (requestDto.getTitle() != null
                        ? requestDto.getTitle() : "null")
                        + ", CareTypeLabel: " + (requestDto.getCareTypeLabel() != null
                        ? requestDto.getCareTypeLabel() : "null"));
        
        try {
            // Set the owner username from the authenticated user
            requestDto.setOwnerUsername(username);
            
            kafkaLogService.debug("AnnouncementController",
                    "Calling service to create announcement for user: "
                            + username);

            Announcement createdAnnouncement =
                    announcementService
                            .createAnnouncementFromDto(requestDto);
            
            kafkaLogService.debug("AnnouncementController",
                    "Announcement created by service. ID: "
                            + (createdAnnouncement != null
                            ? createdAnnouncement.getId() : "null"));
            
            AnnouncementResponseDto responseDto =
                    announcementMapper.toResponseDto(createdAnnouncement);
            
            kafkaLogService.info("AnnouncementController",
                    "Announcement created successfully. ID: "
                            + createdAnnouncement.getId()
                            + ", Owner: " + username
                            + ", Public images: "
                            + (requestDto.getPublicImages() != null
                            ? requestDto.getPublicImages().size() : 0)
                            + ", Specific images: "
                            + (requestDto.getSpecificImages() != null
                            ? requestDto.getSpecificImages().size() : 0));
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(responseDto);
        } catch (IllegalArgumentException e) {
            kafkaLogService.error("AnnouncementController",
                    "Failed to create announcement for user: " + username
                            + ", Error: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            kafkaLogService.error("AnnouncementController",
                    "Unexpected error creating announcement for user: "
                            + username + ", Error: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Update an existing announcement.
     * PUT /api/announcements/{id}
     *
     * @param id the announcement id
     * @param requestDto the updated announcement request DTO
     * @return the updated announcement
     */
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponseDto> updateAnnouncement(
            @PathVariable final Long id,
            @RequestBody final AnnouncementRequestDto requestDto) {
        kafkaLogService.info("AnnouncementController",
                "Updating announcement ID: " + id);
        try {
            Announcement updatedAnnouncement =
                    announcementService
                            .updateAnnouncementFromDto(id, requestDto);
            AnnouncementResponseDto responseDto =
                    announcementMapper.toResponseDto(updatedAnnouncement);
            
            kafkaLogService.info("AnnouncementController",
                    "Announcement updated successfully. ID: " + id
                            + ", Public images: "
                            + (requestDto.getPublicImages() != null
                            ? requestDto.getPublicImages().size() : 0)
                            + ", Specific images: "
                            + (requestDto.getSpecificImages() != null
                            ? requestDto.getSpecificImages().size() : 0));
            
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            kafkaLogService.error("AnnouncementController",
                    "Failed to update announcement ID: " + id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Change the status of an announcement.
     * PATCH /api/announcements/{id}/status
     *
     * @param id the announcement id
     * @param status the new status
     * @return the updated announcement
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AnnouncementResponseDto> changeStatus(
            @PathVariable final Long id,
            @RequestParam final AnnouncementStatus status) {
        kafkaLogService.info("AnnouncementController",
                "Changing status of announcement ID: " + id
                        + " to: " + status);
        try {
            Announcement updatedAnnouncement =
                    announcementService
                            .changeAnnouncementStatus(id, status);
            AnnouncementResponseDto responseDto =
                    announcementMapper.toResponseDto(updatedAnnouncement);
            
            kafkaLogService.info("AnnouncementController",
                    "Announcement status changed successfully. ID: " + id
                            + ", New status: " + status);
            
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            kafkaLogService.error("AnnouncementController",
                    "Failed to change status of announcement ID: " + id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete an announcement.
     * DELETE /api/announcements/{id}
     *
     * @param id the announcement id
     * @return HTTP 204 No Content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable final Long id) {
        kafkaLogService.info("AnnouncementController",
                "Deleting announcement ID: " + id);
        try {
            announcementService.deleteAnnouncement(id);
            
            kafkaLogService.info("AnnouncementController",
                    "Announcement deleted successfully. ID: " + id);
            
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            kafkaLogService.error("AnnouncementController",
                    "Failed to delete announcement ID: " + id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get an announcement by id.
     * GET /api/announcements/{id}
     *
     * @param id the announcement id
     * @param username the username of the user requesting the announcement
     * @return the announcement if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponseDto> getById(
            @RequestHeader("X-Username") final String username,
            @PathVariable final Long id) {
        kafkaLogService.debug("AnnouncementController",
                "Getting announcement ID: " + id
                        + " for user: " + username);
        try {
            Announcement announcement =
                    announcementService.getAnnouncementById(id, username);
            AnnouncementResponseDto responseDto =
                    announcementMapper.toResponseDto(announcement);
            
            kafkaLogService.debug("AnnouncementController",
                    "Announcement retrieved successfully. ID: " + id
                            + ", Public images: "
                            + (responseDto.getPublicImages() != null
                            ? responseDto.getPublicImages().size() : 0)
                            + ", Specific images: "
                            + (responseDto.getSpecificImages() != null
                            ? responseDto.getSpecificImages().size() : 0));
            
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            kafkaLogService.warn("AnnouncementController",
                    "Announcement not found. ID: " + id
                            + ", User: " + username);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all announcements.
     * GET /api/announcements
     *
     * @param ownerUsername the owner username
     * @param status the announcement status
     * @return list of all announcements
     */
    @GetMapping
    public ResponseEntity<List<AnnouncementResponseDto>> getAll(
            @RequestParam(required = false) final String ownerUsername,
            @RequestParam(required = false)
            final AnnouncementStatus status) {

        // If no filters, return all with public images
        if (ownerUsername == null && status == null) {
            List<AnnouncementResponseDto> responseDtos =
                    announcementService.getAllAnnouncementsWithPublicImages();
            return ResponseEntity.ok(responseDtos);
        }

        // Otherwise use the filtered approach
        List<Announcement> announcements;

        if (ownerUsername != null && status != null) {
            announcements =
                    announcementService
                            .getAnnouncementsByOwnerUsernameAndStatus(
                                    ownerUsername, status);
        } else if (ownerUsername != null) {
            announcements =
                    announcementService
                            .getAnnouncementsByOwnerUsername(ownerUsername);
        } else {
            announcements =
                    announcementService
                            .getAnnouncementsByStatus(status);
        }

        List<AnnouncementResponseDto> responseDtos =
                announcementMapper.toResponseDtoList(announcements);
        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Get announcements by owner username.
     * GET /api/announcements/owner/{ownerUsername}
     *
     * @param ownerUsername the owner username
     * @return list of announcements for the owner
     */
    @GetMapping("/owner/{ownerUsername}")
    public ResponseEntity<List<AnnouncementResponseDto>>
            getByOwner(
                    @PathVariable final String ownerUsername) {
        List<Announcement> announcements =
                announcementService.getAnnouncementsByOwnerUsername(
                        ownerUsername);
        List<AnnouncementResponseDto> responseDtos =
                announcementMapper.toResponseDtoList(announcements);
        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Get announcements by status.
     * GET /api/announcements/status/{status}
     *
     * @param status the announcement status
     * @return list of announcements with the specified status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<AnnouncementResponseDto>> getByStatus(
            @PathVariable final AnnouncementStatus status) {
        List<Announcement> announcements =
                announcementService.getAnnouncementsByStatus(status);
        List<AnnouncementResponseDto> responseDtos =
                announcementMapper.toResponseDtoList(announcements);
        return ResponseEntity.ok(responseDtos);
    }
}
