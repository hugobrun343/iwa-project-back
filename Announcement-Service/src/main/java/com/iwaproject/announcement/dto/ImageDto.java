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
     * Image blob data encoded by Jackson as base64 in JSON payloads.
     */
    private byte[] imageBlob;
}

