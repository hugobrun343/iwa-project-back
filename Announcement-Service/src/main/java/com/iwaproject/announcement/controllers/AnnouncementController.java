package com.iwaproject.announcement.controllers;

import com.iwaproject.announcement.dto.AnnouncementMapper;
import com.iwaproject.announcement.dto.AnnouncementRequestDto;
import com.iwaproject.announcement.dto.AnnouncementResponseDto;
import com.iwaproject.announcement.entities.Announcement;
import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import com.iwaproject.announcement.services.AnnouncementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Create a new announcement.
     * POST /api/announcements
     *
     * @param requestDto the announcement request DTO
     * @return the created announcement with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<AnnouncementResponseDto> createAnnouncement(
            @RequestBody final AnnouncementRequestDto requestDto) {
        try {
            Announcement createdAnnouncement =
                    announcementService
                            .createAnnouncementFromDto(requestDto);
            AnnouncementResponseDto responseDto =
                    announcementMapper.toResponseDto(createdAnnouncement);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing announcement.
     * PUT /api/announcements/{id}
     *
     * @param id the announcement id
     * @param announcement the updated announcement data
     * @return the updated announcement
     */
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponseDto> updateAnnouncement(
            @PathVariable final Long id,
            @RequestBody final Announcement announcement) {
        try {
            Announcement updatedAnnouncement =
                    announcementService
                            .updateAnnouncement(id, announcement);
            AnnouncementResponseDto responseDto =
                    announcementMapper.toResponseDto(updatedAnnouncement);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
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
        try {
            Announcement updatedAnnouncement =
                    announcementService
                            .changeAnnouncementStatus(id, status);
            AnnouncementResponseDto responseDto =
                    announcementMapper.toResponseDto(updatedAnnouncement);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
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
        try {
            announcementService.deleteAnnouncement(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get an announcement by id.
     * GET /api/announcements/{id}
     *
     * @param id the announcement id
     * @return the announcement if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponseDto> getById(
            @RequestHeader("X-Username") final String username,
            @PathVariable final Long id) {
        try {
            Announcement announcement =
                    announcementService.getAnnouncementById(id, username);
            AnnouncementResponseDto responseDto =
                    announcementMapper.toResponseDto(announcement);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
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
     * Get announcements by owner id.
     * GET /api/announcements/owner/{ownerId}
     *
     * @param ownerUsername the owner username
     * @return list of announcements for the owner
     */
    @GetMapping("/owner/{ownerUsername}")
    public ResponseEntity<List<AnnouncementResponseDto>> getByOwnerId(
            @PathVariable final String ownerUsername) {
        List<Announcement> announcements =
                announcementService.getAnnouncementsByOwnerUsername(ownerUsername);
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
