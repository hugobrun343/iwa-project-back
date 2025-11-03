package com.iwaproject.application.dtos;

import com.iwaproject.application.entities.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating application status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationUpdateStatusDto {

    /**
     * New application status.
     */
    private ApplicationStatus status;
}
