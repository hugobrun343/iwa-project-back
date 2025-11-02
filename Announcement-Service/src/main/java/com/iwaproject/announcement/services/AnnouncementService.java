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
     * The application verification Kafka service.
     */
    private final ApplicationVerificationKafkaService
            applicationVerificationKafkaService;

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
     * @param username the username of the user requesting the announcement
     * @return the announcement if found
     */
    @Transactional(readOnly = true)
    public Announcement getAnnouncementById(
            final Long id,
            final String username)
    throws IllegalArgumentException {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Announcement not found with id: " + id));

        // verify if the user is the owner of the announcement
        // or has an accepted application for it
        boolean isOwner = username != null
                && username.equals(announcement.getOwnerUsername());
        boolean hasAccess = isOwner || hasUserAcceptedApplication(
                username, announcement.getId());

        // Attach the images to the announcement before returning
        // Load images related to the announcement. If the user has no
        // access, only load public images from the repository to avoid
        // fetching private records and to improve performance.
        List<Image> images;
        if (!hasAccess) {
            // Remove sensitive instructions for non-authorized users
            announcement.removeSpecificInstructions();
            images = imageRepository
                    .findByAnnouncementIdAndIsPrivateFalse(
                            announcement.getId());
        } else {
            images = imageRepository.findByAnnouncementId(
                    announcement.getId());
        }

        // Attach the images to the announcement before returning
        announcement.setImages(images);
        return announcement;
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
                            imageRepository
                                    .findByAnnouncementIdAndIsPrivateFalse(
                                            announcement.getId());
                    return announcementMapper.toResponseDto(
                            announcement, publicImages);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get announcements by owner id.
     * @param ownerUsername the owner username
     * @return list of announcements for the owner
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsByOwnerUsername(
            final String ownerUsername) {
        return announcementRepository.findByOwnerUsername(ownerUsername);
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
     * @param ownerUsername the owner username
     * @param status the announcement status
     * @return list of announcements for the owner with the specified
     * status
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsByOwnerUsernameAndStatus(
            final String ownerUsername, final AnnouncementStatus status) {
        return announcementRepository.findByOwnerUsernameAndStatus(
                ownerUsername, status);
    }

    /**
     * Get public images for an announcement.
     * @param announcementId the announcement ID
     * @return list of public images
     */
    @Transactional(readOnly = true)
    public List<Image> getPublicImagesByAnnouncement(
            final Long announcementId) {
        return imageRepository
                .findByAnnouncementIdAndIsPrivateFalse(announcementId);
    }

    /**
     * Get all images (including private) for an announcement.
     * @param announcementId the announcement ID
     * @return list of all images
     */
    @Transactional(readOnly = true)
    public List<Image> getPrivateImagesByAnnouncement(
            final Long announcementId) {
        return imageRepository.findByAnnouncementId(
                announcementId);
    }

    /**
     * Check if user has an accepted application for an announcement.
     * @param username the username
     * @param id the announcement ID
     * @return true if user has accepted application, false otherwise
     */
    private boolean hasUserAcceptedApplication(
            final String username, final Long id) {
        // Use Kafka to asynchronously verify if user has accepted
        // application. Wait for the result with a timeout (blocking here
        // is acceptable as we need the result to proceed)
        try {
            return applicationVerificationKafkaService
                    .hasUserAcceptedApplication(username, id)
                    .get(); // Blocks until response received or timeout
        } catch (Exception e) {
            System.err.println("⚠️ Error verifying application: "
                    + e.getMessage());
            return false;
        }
    }
}
