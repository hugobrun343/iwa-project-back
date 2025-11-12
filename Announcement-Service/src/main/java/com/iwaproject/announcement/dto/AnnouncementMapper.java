package com.iwaproject.announcement.dto;

import com.iwaproject.announcement.entities.Announcement;
import com.iwaproject.announcement.entities.CareType;
import com.iwaproject.announcement.entities.Image;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Announcement entities and DTOs.
 */
@Component
public class AnnouncementMapper {

    /**
     * Convert AnnouncementRequestDto to Announcement entity.
     *
     * @param dto the announcement request DTO
     * @param careType the care type entity
     * @return the announcement entity
     */
    public Announcement toEntity(final AnnouncementRequestDto dto,
                                   final CareType careType) {
        if (dto == null) {
            return null;
        }

        Announcement announcement = new Announcement();
        announcement.setOwnerUsername(dto.getOwnerUsername());
        announcement.setTitle(dto.getTitle());
        announcement.setLocation(dto.getLocation());
        announcement.setDescription(dto.getDescription());
        announcement.setSpecificInstructions(
                dto.getSpecificInstructions());
        announcement.setCareType(careType);
        announcement.setStartDate(dto.getStartDate());
        announcement.setEndDate(dto.getEndDate());
        announcement.setVisitFrequency(dto.getVisitFrequency());
        announcement.setRemuneration(dto.getRemuneration());
        announcement.setIdentityVerificationRequired(
                dto.getIdentityVerificationRequired());
        announcement.setUrgentRequest(dto.getUrgentRequest());
        announcement.setStatus(dto.getStatus());
        announcement.setCreationDate(LocalDateTime.now());

        return announcement;
    }

    /**
     * Convert Announcement entity to AnnouncementResponseDto.
     *
     * @param announcement the announcement entity
     * @return the announcement response DTO
     */
    public AnnouncementResponseDto toResponseDto(
            final Announcement announcement) {
        if (announcement == null) {
            return null;
        }

        AnnouncementResponseDto dto = new AnnouncementResponseDto();
        dto.setId(announcement.getId());
        dto.setOwnerUsername(announcement.getOwnerUsername());
        dto.setTitle(announcement.getTitle());
        dto.setLocation(announcement.getLocation());
        dto.setDescription(announcement.getDescription());
        dto.setSpecificInstructions(
                announcement.getSpecificInstructions());
        dto.setCareType(toCareTypeDto(announcement.getCareType()));
        dto.setStartDate(announcement.getStartDate());
        dto.setEndDate(announcement.getEndDate());
        dto.setVisitFrequency(announcement.getVisitFrequency());
        dto.setRemuneration(announcement.getRemuneration());
        dto.setIdentityVerificationRequired(
                announcement.getIdentityVerificationRequired());
        dto.setUrgentRequest(announcement.getUrgentRequest());
        dto.setStatus(announcement.getStatus());
        dto.setCreationDate(announcement.getCreationDate());

        // Split images into public and specific based on isPrivate flag
        if (announcement.getImages() != null) {
            List<Image> publicImages = announcement.getImages().stream()
                    .filter(image -> image.getIsPrivate() != null
                            && !image.getIsPrivate())
                    .collect(Collectors.toList());
            List<Image> specificImages = announcement.getImages().stream()
                    .filter(image -> image.getIsPrivate() != null
                            && image.getIsPrivate())
                    .collect(Collectors.toList());

            dto.setPublicImages(toImageDtoList(publicImages));
            dto.setSpecificImages(toImageDtoList(specificImages));
        }

        return dto;
    }

    /**
     * Convert Announcement entity to AnnouncementResponseDto with images.
     *
     * @param announcement the announcement entity
     * @param publicImages the list of public images
     * @return the announcement response DTO
     */
    public AnnouncementResponseDto toResponseDto(
            final Announcement announcement,
            final List<Image> publicImages) {
        if (announcement == null) {
            return null;
        }

        AnnouncementResponseDto dto = new AnnouncementResponseDto();
        dto.setId(announcement.getId());
        dto.setOwnerUsername(announcement.getOwnerUsername());
        dto.setTitle(announcement.getTitle());
        dto.setLocation(announcement.getLocation());
        dto.setDescription(announcement.getDescription());
        dto.setSpecificInstructions(
                announcement.getSpecificInstructions());
        dto.setCareType(toCareTypeDto(announcement.getCareType()));
        dto.setStartDate(announcement.getStartDate());
        dto.setEndDate(announcement.getEndDate());
        dto.setVisitFrequency(announcement.getVisitFrequency());
        dto.setRemuneration(announcement.getRemuneration());
        dto.setIdentityVerificationRequired(
                announcement.getIdentityVerificationRequired());
        dto.setUrgentRequest(announcement.getUrgentRequest());
        dto.setStatus(announcement.getStatus());
        dto.setCreationDate(announcement.getCreationDate());

        // Add public images if provided
        if (publicImages != null) {
            dto.setPublicImages(toImageDtoList(publicImages));
        }

        return dto;
    }

    /**
     * Convert list of Announcement entities to list
     * of AnnouncementResponseDto.
     *
     * @param announcements the list of announcement entities
     * @return the list of announcement response DTOs
     */
    public List<AnnouncementResponseDto> toResponseDtoList(
            final List<Announcement> announcements) {
        return announcements.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert CareType entity to CareTypeDto.
     *
     * @param careType the care type entity
     * @return the care type DTO
     */
    private CareTypeDto toCareTypeDto(final CareType careType) {
        if (careType == null) {
            return null;
        }

        CareTypeDto dto = new CareTypeDto();
        dto.setId(careType.getId());
        dto.setLabel(careType.getLabel());

        return dto;
    }

    /**
     * Convert Image entity to ImageDto.
     *
     * @param image the image entity
     * @return the image DTO
     */
    private ImageDto toImageDto(final Image image) {
        if (image == null) {
            return null;
        }

        ImageDto dto = new ImageDto();
        dto.setId(image.getId());
        dto.setImageUrl(image.getImageUrl());

        return dto;
    }

    /**
     * Convert list of Image entities to list of ImageDto.
     *
     * @param images the list of image entities
     * @return the list of image DTOs
     */
    private List<ImageDto> toImageDtoList(final List<Image> images) {
        if (images == null) {
            return null;
        }
        return images.stream()
                .map(this::toImageDto)
                .collect(Collectors.toList());
    }
}
