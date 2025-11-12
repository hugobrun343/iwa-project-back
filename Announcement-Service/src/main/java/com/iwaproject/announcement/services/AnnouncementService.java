package com.iwaproject.announcement.services;

import com.iwaproject.announcement.dto.AnnouncementRequestDto;
import com.iwaproject.announcement.dto.AnnouncementMapper;
import com.iwaproject.announcement.dto.AnnouncementResponseDto;
import com.iwaproject.announcement.dto.ImageDto;
import com.iwaproject.announcement.entities.Announcement;
import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import com.iwaproject.announcement.entities.CareType;
import com.iwaproject.announcement.entities.Image;
import com.iwaproject.announcement.repositories.AnnouncementRepository;
import com.iwaproject.announcement.repositories.CareTypeRepository;
import java.util.ArrayList;
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
     * The Kafka log service.
     */
    private final KafkaLogService kafkaLogService;

    /**
     * Create a new announcement from DTO.
     * @param requestDto the announcement request DTO
     * @return the created announcement
     * @throws IllegalArgumentException if the care type does not exist
     */
    public Announcement createAnnouncementFromDto(
            final AnnouncementRequestDto requestDto) {
        kafkaLogService.info("AnnouncementService",
                "=== START createAnnouncementFromDto === Owner: "
                        + requestDto.getOwnerUsername()
                        + ", CareTypeLabel: " + requestDto.getCareTypeLabel());
        
        // Find care type by label
        String careTypeLabel = requestDto.getCareTypeLabel();
        kafkaLogService.debug("AnnouncementService",
                "Looking for care type with label: " + careTypeLabel);
        
        CareType careType =
                careTypeRepository.findByLabel(careTypeLabel)
                .orElseThrow(() -> {
                    // Get all available care types for better error message
                    List<CareType> allCareTypes = careTypeRepository.findAll();
                    String availableLabels = allCareTypes.stream()
                            .map(CareType::getLabel)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("none");
                    
                    String errorMessage = "Care type not found with label: '"
                            + careTypeLabel
                            + "'. Available care types: " + availableLabels;
                    
                    kafkaLogService.error("AnnouncementService",
                            errorMessage);
                    return new IllegalArgumentException(errorMessage);
                });
        
        kafkaLogService.debug("AnnouncementService",
                "Care type found. ID: " + careType.getId()
                        + ", Label: " + careType.getLabel());

        // Convert DTO to entity
        kafkaLogService.debug("AnnouncementService",
                "Converting DTO to entity");
        Announcement announcement =
                announcementMapper.toEntity(requestDto, careType);
        
        kafkaLogService.debug("AnnouncementService",
                "Entity created. Title: " + announcement.getTitle()
                        + ", Location: " + announcement.getLocation());

        // Set default status if not provided
        if (announcement.getStatus() == null) {
            announcement.setStatus(AnnouncementStatus.PUBLISHED);
            kafkaLogService.debug("AnnouncementService",
                    "Status set to default: PUBLISHED");
        }

        // Save the announcement first to get the ID
        kafkaLogService.debug("AnnouncementService",
                "Saving announcement to database");
        Announcement savedAnnouncement = announcementRepository.save(announcement);
        
        kafkaLogService.info("AnnouncementService",
                "Announcement saved to database. ID: "
                        + savedAnnouncement.getId());

        // Save images from DTO
        saveImagesFromDto(savedAnnouncement, requestDto);

        // Load images to include them in the response
        List<Image> images = imageRepository.findByAnnouncementId(
                savedAnnouncement.getId());
        savedAnnouncement.setImages(images);

        kafkaLogService.info("AnnouncementService",
                "Announcement created successfully. ID: "
                        + savedAnnouncement.getId()
                        + ", Total images: " + images.size());

        return savedAnnouncement;
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
     * Update an existing announcement from DTO.
     * @param id the announcement id
     * @param requestDto the updated announcement request DTO
     * @return the updated announcement
     * @throws IllegalArgumentException if the announcement or care type
     * does not exist
     */
    public Announcement updateAnnouncementFromDto(
            final Long id,
            final AnnouncementRequestDto requestDto) {
        kafkaLogService.debug("AnnouncementService",
                "Updating announcement from DTO. ID: " + id);
        
        Announcement existingAnnouncement =
                announcementRepository.findById(id)
                .orElseThrow(() -> {
                    kafkaLogService.error("AnnouncementService",
                            "Announcement not found with id: " + id);
                    return new IllegalArgumentException(
                            "Announcement not found with id: " + id);
                });

        // Update care type if provided
        if (requestDto.getCareTypeLabel() != null) {
            String careTypeLabel = requestDto.getCareTypeLabel();
            CareType careType =
                    careTypeRepository.findByLabel(careTypeLabel)
                    .orElseThrow(() -> {
                        // Get all available care types for better error message
                        List<CareType> allCareTypes =
                                careTypeRepository.findAll();
                        String availableLabels = allCareTypes.stream()
                                .map(CareType::getLabel)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("none");
                        
                        String errorMessage = "Care type not found with label: '"
                                + careTypeLabel
                                + "'. Available care types: " + availableLabels;
                        
                        kafkaLogService.error("AnnouncementService",
                                errorMessage);
                        return new IllegalArgumentException(errorMessage);
                    });
            existingAnnouncement.setCareType(careType);
        }

        // Update fields only if they are not null
        if (requestDto.getTitle() != null) {
            existingAnnouncement.setTitle(requestDto.getTitle());
        }
        if (requestDto.getLocation() != null) {
            existingAnnouncement.setLocation(requestDto.getLocation());
        }
        if (requestDto.getDescription() != null) {
            existingAnnouncement.setDescription(
                    requestDto.getDescription());
        }
        if (requestDto.getSpecificInstructions() != null) {
            existingAnnouncement.setSpecificInstructions(
                    requestDto.getSpecificInstructions());
        }
        if (requestDto.getStartDate() != null) {
            existingAnnouncement.setStartDate(requestDto.getStartDate());
        }
        if (requestDto.getEndDate() != null) {
            existingAnnouncement.setEndDate(requestDto.getEndDate());
        }
        if (requestDto.getVisitFrequency() != null) {
            existingAnnouncement.setVisitFrequency(
                    requestDto.getVisitFrequency());
        }
        if (requestDto.getRemuneration() != null) {
            existingAnnouncement.setRemuneration(
                    requestDto.getRemuneration());
        }
        if (requestDto.getIdentityVerificationRequired() != null) {
            existingAnnouncement.setIdentityVerificationRequired(
                    requestDto.getIdentityVerificationRequired());
        }
        if (requestDto.getUrgentRequest() != null) {
            existingAnnouncement.setUrgentRequest(
                    requestDto.getUrgentRequest());
        }
        if (requestDto.getStatus() != null) {
            existingAnnouncement.setStatus(requestDto.getStatus());
        }

        // Save the announcement
        Announcement savedAnnouncement =
                announcementRepository.save(existingAnnouncement);
        
        kafkaLogService.info("AnnouncementService",
                "Announcement updated in database. ID: " + id);

        // Update images from DTO
        saveImagesFromDto(savedAnnouncement, requestDto);

        // Load images to include them in the response
        List<Image> images = imageRepository.findByAnnouncementId(
                savedAnnouncement.getId());
        savedAnnouncement.setImages(images);

        kafkaLogService.info("AnnouncementService",
                "Announcement updated successfully. ID: " + id
                        + ", Total images: " + images.size());

        return savedAnnouncement;
    }

    /**
     * Save images from DTO to the database.
     * This method deletes existing images for the announcement and
     * saves new ones from the DTO.
     *
     * @param announcement the announcement entity
     * @param requestDto the announcement request DTO containing images
     */
    private void saveImagesFromDto(
            final Announcement announcement,
            final AnnouncementRequestDto requestDto) {
        kafkaLogService.debug("AnnouncementService",
                "Saving images for announcement ID: "
                        + announcement.getId());
        
        // Delete existing images for this announcement
        List<Image> existingImages =
                imageRepository.findByAnnouncementId(announcement.getId());
        if (!existingImages.isEmpty()) {
            imageRepository.deleteAll(existingImages);
            kafkaLogService.debug("AnnouncementService",
                    "Deleted " + existingImages.size()
                            + " existing images for announcement ID: "
                            + announcement.getId());
        }

        // Save new images
        List<Image> imagesToSave = new ArrayList<>();
        int publicImagesCount = 0;
        int specificImagesCount = 0;

        // Save public images (isPrivate = false)
        if (requestDto.getPublicImages() != null) {
            for (ImageDto imageDto : requestDto.getPublicImages()) {
                if (imageDto != null && imageDto.getImageUrl() != null) {
                    Image image = new Image();
                    image.setAnnouncement(announcement);
                    image.setImageUrl(imageDto.getImageUrl());
                    image.setIsPrivate(false);
                    imagesToSave.add(image);
                    publicImagesCount++;
                }
            }
        }

        // Save specific images (isPrivate = true)
        if (requestDto.getSpecificImages() != null) {
            for (ImageDto imageDto : requestDto.getSpecificImages()) {
                if (imageDto != null && imageDto.getImageUrl() != null) {
                    Image image = new Image();
                    image.setAnnouncement(announcement);
                    image.setImageUrl(imageDto.getImageUrl());
                    image.setIsPrivate(true);
                    imagesToSave.add(image);
                    specificImagesCount++;
                }
            }
        }

        // Save all images
        if (!imagesToSave.isEmpty()) {
            imageRepository.saveAll(imagesToSave);
            kafkaLogService.info("AnnouncementService",
                    "Saved images for announcement ID: "
                            + announcement.getId()
                            + ", Public: " + publicImagesCount
                            + ", Specific: " + specificImagesCount);
        } else {
            kafkaLogService.debug("AnnouncementService",
                    "No images to save for announcement ID: "
                            + announcement.getId());
        }
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
        kafkaLogService.debug("AnnouncementService",
                "Changing announcement status. ID: " + id
                        + ", New status: " + newStatus);
        
        Announcement announcement =
                announcementRepository.findById(id)
                .orElseThrow(() -> {
                    kafkaLogService.error("AnnouncementService",
                            "Announcement not found with id: " + id);
                    return new IllegalArgumentException(
                            "Announcement not found with id: " + id);
                });

        announcement.setStatus(newStatus);
        Announcement saved = announcementRepository.save(announcement);
        
        kafkaLogService.info("AnnouncementService",
                "Announcement status changed. ID: " + id
                        + ", Status: " + newStatus);
        
        return saved;
    }

    /**
     * Delete an announcement.
     * @param id the announcement id
     * @throws IllegalArgumentException if the announcement does not exist
     */
    public void deleteAnnouncement(final Long id) {
        kafkaLogService.debug("AnnouncementService",
                "Deleting announcement. ID: " + id);
        
        if (!announcementRepository.existsById(id)) {
            kafkaLogService.error("AnnouncementService",
                    "Announcement not found with id: " + id);
            throw new IllegalArgumentException(
                    "Announcement not found with id: " + id);
        }
        
        announcementRepository.deleteById(id);
        
        kafkaLogService.info("AnnouncementService",
                "Announcement deleted successfully. ID: " + id);
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
        kafkaLogService.debug("AnnouncementService",
                "Getting announcement by ID. ID: " + id
                        + ", User: " + username);
        
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> {
                    kafkaLogService.warn("AnnouncementService",
                            "Announcement not found with id: " + id);
                    return new IllegalArgumentException(
                            "Announcement not found with id: " + id);
                });

        // verify if the user is the owner of the announcement
        // or has an accepted application for it
        boolean isOwner = username != null
                && username.equals(announcement.getOwnerUsername());
        boolean hasAccess = isOwner || hasUserAcceptedApplication(
                username, announcement.getId());

        kafkaLogService.debug("AnnouncementService",
                "Access check for announcement ID: " + id
                        + ", User: " + username
                        + ", IsOwner: " + isOwner
                        + ", HasAccess: " + hasAccess);

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
            kafkaLogService.debug("AnnouncementService",
                    "Loaded public images only for announcement ID: " + id
                            + ", Count: " + images.size());
        } else {
            images = imageRepository.findByAnnouncementId(
                    announcement.getId());
            kafkaLogService.debug("AnnouncementService",
                    "Loaded all images for announcement ID: " + id
                            + ", Count: " + images.size());
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
            return false;
        }
    }
}
