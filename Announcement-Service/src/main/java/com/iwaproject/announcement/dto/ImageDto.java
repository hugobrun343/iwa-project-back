package com.iwaproject.announcement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Image.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageDto {
    /**
     * Id.
     */
    private Long id;

    /**
     * Image URL.
     */
    private String imageUrl;
}

