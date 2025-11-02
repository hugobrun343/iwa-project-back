package com.iwaproject.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageDTO {

    /**
     * Message content.
     */
    private String content;
}

