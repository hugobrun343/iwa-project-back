package com.iwaproject.announcement.services;

import com.iwaproject.announcement.dto.AnnouncementRequestDto;
import com.iwaproject.announcement.dto.AnnouncementMapper;
import com.iwaproject.announcement.dto.AnnouncementResponseDto;
import com.iwaproject.announcement.entities.Announcement;
import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import com.iwaproject.announcement.entities.CareType;
import com.iwaproject.announcement.entities.Image;
import com.iwaproject.announcement.repositories.AnnouncementRepository;
import com.iwaproject.announcement.repositories.CareTypeRepository;
import java.util.List;
import java.util.Optional;

import com.iwaproject.announcement.repositories.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing announcements.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {

    /**
     * The announcement repository.
     */
    private final AnnouncementRepository announcementRepository;

    /**
     * The care type repository.
     */
    private final CareTypeRepository careTypeRepository;

    /**
     * The image repository.
     */
    private final ImageRepository imageRepository;

    /**
     * The announcement mapper.
     */
    private final AnnouncementMapper announcementMapper;

    /**
     * Create a new announcement from DTO.
     * @param requestDto the announcement request DTO
     * @return the created announcement
     * @throws IllegalArgumentException if the care type does not exist
     */
    public Announcement createAnnouncementFromDto(
            final AnnouncementRequestDto requestDto) {
        // Find care type by label
        CareType careType =
                careTypeRepository.findByLabel(
                        requestDto.getCareTypeLabel())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Care type not found with label: "
                                + requestDto.getCareTypeLabel()));

        // Convert DTO to entity
        Announcement announcement =
                announcementMapper.toEntity(requestDto, careType);

        // Set default status if not provided
        if (announcement.getStatus() == null) {
            announcement.setStatus(AnnouncementStatus.PUBLISHED);
        }

        return announcementRepository.save(announcement);
    }

    /**
     * Create a new announcement.
     * @param announcement the announcement to create
     * @return the created announcement
     * @throws IllegalArgumentException if the care type does not exist
     */
    public Announcement createAnnouncement(
            final Announcement announcement) {
        // Validate that the care type exists
        if (announcement.getCareType() != null
                && announcement.getCareType().getId() != null) {
            CareType careType =
                    careTypeRepository.findById(
                            announcement.getCareType().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Care type not found with id: "
                                    + announcement.getCareType().getId()));
            announcement.setCareType(careType);
        } else {
            throw new IllegalArgumentException(
                    "Care type is required");
        }

        // Set default status if not provided
        if (announcement.getStatus() == null) {
            announcement.setStatus(AnnouncementStatus.PUBLISHED);
        }

        return announcementRepository.save(announcement);
    }

    /**
     * Update an existing announcement.
     * @param id the announcement id
     * @param updatedAnnouncement the updated announcement data
     * @return the updated announcement
     * @throws IllegalArgumentException if the announcement or care type
     * does not exist
     */
    public Announcement updateAnnouncement(
            final Long id,
            final Announcement updatedAnnouncement) {
        Announcement existingAnnouncement =
                announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Announcement not found with id: " + id));

        // Update fields only if they are not null
        if (updatedAnnouncement.getTitle() != null) {
            existingAnnouncement.setTitle(
                    updatedAnnouncement.getTitle());
        }
        if (updatedAnnouncement.getLocation() != null) {
            existingAnnouncement.setLocation(
                    updatedAnnouncement.getLocation());
        }
        if (updatedAnnouncement.getDescription() != null) {
            existingAnnouncement.setDescription(
                    updatedAnnouncement.getDescription());
        }
        if (updatedAnnouncement.getSpecificInstructions() != null) {
            existingAnnouncement.setSpecificInstructions(
                    updatedAnnouncement.getSpecificInstructions());
        }
        if (updatedAnnouncement.getStartDate() != null) {
            existingAnnouncement.setStartDate(
                    updatedAnnouncement.getStartDate());
        }
        if (updatedAnnouncement.getEndDate() != null) {
            existingAnnouncement.setEndDate(
                    updatedAnnouncement.getEndDate());
        }
        if (updatedAnnouncement.getVisitFrequency() != null) {
            existingAnnouncement.setVisitFrequency(
                    updatedAnnouncement.getVisitFrequency());
        }
        if (updatedAnnouncement.getRemuneration() != null) {
            existingAnnouncement.setRemuneration(
                    updatedAnnouncement.getRemuneration());
        }
        if (updatedAnnouncement.getIdentityVerificationRequired()
                != null) {
            existingAnnouncement.setIdentityVerificationRequired(
                    updatedAnnouncement
                            .getIdentityVerificationRequired());
        }
        if (updatedAnnouncement.getUrgentRequest() != null) {
            existingAnnouncement.setUrgentRequest(
                    updatedAnnouncement.getUrgentRequest());
        }

        // Update care type if provided
        if (updatedAnnouncement.getCareType() != null
                && updatedAnnouncement.getCareType().getId() != null) {
            CareType careType =
                    careTypeRepository.findById(
                            updatedAnnouncement.getCareType().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Care type not found with id: "
                                    + updatedAnnouncement.getCareType()
                                    .getId()));
            existingAnnouncement.setCareType(careType);
        }

        return announcementRepository.save(existingAnnouncement);
    }

    /**
     * Change the status of an announcement.
     * @param id the announcement id
     * @param newStatus the new status
     * @return the updated announcement
     * @throws IllegalArgumentException if the announcement does not exist
     */
    public Announcement changeAnnouncementStatus(
            final Long id,
            final AnnouncementStatus newStatus) {
        Announcement announcement =
                announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Announcement not found with id: " + id));

        announcement.setStatus(newStatus);
        return announcementRepository.save(announcement);
    }

    /**
     * Delete an announcement.
     * @param id the announcement id
     * @throws IllegalArgumentException if the announcement does not exist
     */
    public void deleteAnnouncement(final Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Announcement not found with id: " + id);
        }
        announcementRepository.deleteById(id);
    }

    /**
     * Get an announcement by id.
     * @param id the announcement id
     * @return the announcement if found
     */
    @Transactional(readOnly = true)
    public Optional<Announcement> getAnnouncementById(final Long id) {
        return announcementRepository.findById(id);
    }

    /**
     * Get all announcements.
     * @return list of all announcements
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    /**
     * Get all announcements with public images.
     * @return list of all announcements with their public images
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponseDto> getAllAnnouncementsWithPublicImages() {
        List<Announcement> announcements = announcementRepository.findAll();
        return announcements.stream()
                .map(announcement -> {
                    List<Image> publicImages =
                            imageRepository.findByAnnouncementIdAndIsPrivateFalse(
                                    announcement.getId());
                    return announcementMapper.toResponseDto(announcement, publicImages);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get announcements by owner id.
     * @param ownerId the owner id
     * @return list of announcements for the owner
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsByOwnerId(
            final Long ownerId) {
        return announcementRepository.findByOwnerId(ownerId);
    }

    /**
     * Get announcements by status.
     * @param status the announcement status
     * @return list of announcements with the specified status
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsByStatus(
            final AnnouncementStatus status) {
        return announcementRepository.findByStatus(status);
    }

    /**
     * Get announcements by owner id and status.
     * @param ownerId the owner id
     * @param status the announcement status
     * @return list of announcements for the owner with the specified
     * status
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsByOwnerIdAndStatus(
            final Long ownerId, final AnnouncementStatus status) {
        return announcementRepository.findByOwnerIdAndStatus(
                ownerId, status);
    }

    @Transactional(readOnly = true)
    public List<Image> getPublicImagesByAnnouncement(final Long announcementId) {
        return imageRepository.findByAnnouncementIdAndIsPrivateFalse(announcementId);
    }

    @Transactional(readOnly = true)
    public List<Image> getPrivateImagesByAnnouncement(final Long announcementId) {
        return imageRepository.findByAnnouncementId(announcementId);
    }
}

