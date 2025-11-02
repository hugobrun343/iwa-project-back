package com.iwaproject.application.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.application.dtos.ApplicationRequestDto;
import com.iwaproject.application.dtos.ApplicationResponseDto;
import com.iwaproject.application.dtos.ApplicationUpdateStatusDto;
import com.iwaproject.application.entities.ApplicationStatus;
import com.iwaproject.application.services.ApplicationService;
import com.iwaproject.application.services.KafkaLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private KafkaLogService kafkaLogService;

    private ApplicationResponseDto responseDto;
    private ApplicationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        responseDto = new ApplicationResponseDto();
        responseDto.setId(1);
        responseDto.setAnnouncementId(100);
        responseDto.setGuardianUsername("guardianUsername");
        responseDto.setStatus(ApplicationStatus.SENT);
        responseDto.setApplicationDate(LocalDateTime.now());

        requestDto = new ApplicationRequestDto();
        requestDto.setAnnouncementId(100);
        requestDto.setGuardianUsername("guardianUsername");
    }

    @Test
    void createApplication_Success() throws Exception {
        when(applicationService.createApplicationDto(
                any(ApplicationRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.announcementId").value(100))
                .andExpect(jsonPath("$.guardianUsername")
                        .value("guardianUsername"))
                .andExpect(jsonPath("$.status").value("SENT"));

        verify(applicationService, times(1)).createApplicationDto(
                any(ApplicationRequestDto.class));
    }

    @Test
    void createApplication_AlreadyExists_ReturnsConflict() throws Exception {
        when(applicationService.createApplicationDto(
                any(ApplicationRequestDto.class)))
                .thenThrow(new IllegalStateException(
                        "Application already exists"));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());

        verify(applicationService, times(1)).createApplicationDto(
                any(ApplicationRequestDto.class));
    }

    @Test
    void getApplicationById_Success() throws Exception {
        when(applicationService.getApplicationById(1)).thenReturn(responseDto);

        mockMvc.perform(get("/api/applications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.announcementId").value(100));

        verify(applicationService, times(1)).getApplicationById(1);
    }

    @Test
    void getApplicationById_NotFound() throws Exception {
        when(applicationService.getApplicationById(999))
                .thenThrow(new IllegalArgumentException("Application not found"));

        mockMvc.perform(get("/api/applications/999"))
                .andExpect(status().isNotFound());

        verify(applicationService, times(1)).getApplicationById(999);
    }

    @Test
    void getAllApplications_Success() throws Exception {
        List<ApplicationResponseDto> applications = Arrays.asList(responseDto);
        when(applicationService.getAllApplications()).thenReturn(applications);

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].announcementId").value(100));

        verify(applicationService, times(1)).getAllApplications();
    }

    @Test
    void getApplicationsByAnnouncementId_Success() throws Exception {
        List<ApplicationResponseDto> applications = Arrays.asList(responseDto);
        when(applicationService.getApplicationsByAnnouncementId(100)).thenReturn(applications);

        mockMvc.perform(get("/api/applications")
                        .param("announcementId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].announcementId").value(100));

        verify(applicationService, times(1)).getApplicationsByAnnouncementId(100);
    }

    @Test
    void getApplicationsByGuardianUsername_Success() throws Exception {
        List<ApplicationResponseDto> applications = Arrays.asList(responseDto);
        when(applicationService.getApplicationsByGuardianUsername("guardianUsername")).thenReturn(applications);

        mockMvc.perform(get("/api/applications")
                        .param("guardianUsername", "guardianUsername"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guardianUsername").value("guardianUsername"));

        verify(applicationService, times(1)).getApplicationsByGuardianUsername("guardianUsername");
    }

    @Test
    void getApplicationsByStatus_Success() throws Exception {
        List<ApplicationResponseDto> applications = Arrays.asList(responseDto);
        when(applicationService.getApplicationsByStatus(ApplicationStatus.SENT)).thenReturn(applications);

        mockMvc.perform(get("/api/applications")
                        .param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SENT"));

        verify(applicationService, times(1)).getApplicationsByStatus(ApplicationStatus.SENT);
    }

    @Test
    void updateApplicationStatus_Success() throws Exception {
        ApplicationUpdateStatusDto updateDto = new ApplicationUpdateStatusDto();
        updateDto.setStatus(ApplicationStatus.ACCEPTED);

        responseDto.setStatus(ApplicationStatus.ACCEPTED);
        when(applicationService.updateApplicationStatus(eq(1), any(ApplicationUpdateStatusDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/applications/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(applicationService, times(1)).updateApplicationStatus(eq(1), any(ApplicationUpdateStatusDto.class));
    }

    @Test
    void deleteApplication_Success() throws Exception {
        doNothing().when(applicationService).deleteApplication(1);

        mockMvc.perform(delete("/api/applications/1"))
                .andExpect(status().isNoContent());

        verify(applicationService, times(1)).deleteApplication(1);
    }

    @Test
    void deleteApplication_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Application not found"))
                .when(applicationService).deleteApplication(999);

        mockMvc.perform(delete("/api/applications/999"))
                .andExpect(status().isNotFound());

        verify(applicationService, times(1)).deleteApplication(999);
    }
}
