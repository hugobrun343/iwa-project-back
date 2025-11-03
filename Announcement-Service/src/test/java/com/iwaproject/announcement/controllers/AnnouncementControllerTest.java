package com.iwaproject.announcement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.announcement.dto.AnnouncementMapper;
import com.iwaproject.announcement.dto.AnnouncementRequestDto;
import com.iwaproject.announcement.dto.AnnouncementResponseDto;
import com.iwaproject.announcement.dto.CareTypeDto;
import com.iwaproject.announcement.entities.Announcement;
import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import com.iwaproject.announcement.entities.CareType;
import com.iwaproject.announcement.services.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AnnouncementController.
 */
@WebMvcTest(AnnouncementController.class)
@DisplayName("AnnouncementController Tests")
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean
    private AnnouncementMapper announcementMapper;

    private Announcement announcement;
    private AnnouncementResponseDto responseDto;
    private AnnouncementRequestDto requestDto;

    @BeforeEach
    void setUp() {
        CareType careType = new CareType(1L, "Soins infirmiers");
        CareTypeDto careTypeDto = new CareTypeDto(1L, "Soins infirmiers");

        announcement = new Announcement();
        announcement.setId(1L);
        announcement.setOwnerUsername("test");
        announcement.setTitle("Recherche infirmier");
        announcement.setLocation("Paris");
        announcement.setDescription("Besoin d'un infirmier");
        announcement.setSpecificInstructions("Instructions spécifiques");
        announcement.setCareType(careType);
        announcement.setStartDate(LocalDate.now().plusDays(1));
        announcement.setEndDate(LocalDate.now().plusDays(30));
        announcement.setVisitFrequency("2 fois par semaine");
        announcement.setRemuneration(50.0f);
        announcement.setIdentityVerificationRequired(true);
        announcement.setUrgentRequest(false);
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setCreationDate(LocalDateTime.now());

        responseDto = new AnnouncementResponseDto();
        responseDto.setId(1L);
        responseDto.setOwnerUsername("test");
        responseDto.setTitle("Recherche infirmier");
        responseDto.setLocation("Paris");
        responseDto.setDescription("Besoin d'un infirmier");
        responseDto.setSpecificInstructions("Instructions spécifiques");
        responseDto.setCareType(careTypeDto);
        responseDto.setStartDate(announcement.getStartDate());
        responseDto.setEndDate(announcement.getEndDate());
        responseDto.setVisitFrequency("2 fois par semaine");
        responseDto.setRemuneration(50.0f);
        responseDto.setIdentityVerificationRequired(true);
        responseDto.setUrgentRequest(false);
        responseDto.setStatus(AnnouncementStatus.PUBLISHED);
        responseDto.setCreationDate(announcement.getCreationDate());

        requestDto = new AnnouncementRequestDto();
        requestDto.setOwnerUsername("test");
        requestDto.setTitle("Recherche infirmier");
        requestDto.setLocation("Paris");
        requestDto.setDescription("Besoin d'un infirmier");
        requestDto.setSpecificInstructions("Instructions spécifiques");
        requestDto.setCareTypeLabel("Soins infirmiers");
        requestDto.setStartDate(LocalDate.now().plusDays(1));
        requestDto.setEndDate(LocalDate.now().plusDays(30));
        requestDto.setVisitFrequency("2 fois par semaine");
        requestDto.setRemuneration(50.0f);
        requestDto.setIdentityVerificationRequired(true);
        requestDto.setUrgentRequest(false);
        requestDto.setStatus(AnnouncementStatus.PUBLISHED);
    }

    @Test
    @DisplayName("POST /api/announcements - Should create announcement successfully")
    void testCreateAnnouncement_Success() throws Exception {
        // Given
        when(announcementService.createAnnouncementFromDto(any(AnnouncementRequestDto.class))).thenReturn(announcement);
        when(announcementMapper.toResponseDto(any(Announcement.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/announcements")
                        .header("X-Username", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Recherche infirmier"))
                .andExpect(jsonPath("$.location").value("Paris"))
                .andExpect(jsonPath("$.ownerUsername").value("test"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(announcementService).createAnnouncementFromDto(any(AnnouncementRequestDto.class));
        verify(announcementMapper).toResponseDto(any(Announcement.class));
    }

    @Test
    @DisplayName("POST /api/announcements - Should return bad request when service throws exception")
    void testCreateAnnouncement_BadRequest() throws Exception {
        // Given
        when(announcementService.createAnnouncementFromDto(any(AnnouncementRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Care type is required"));

        // When & Then
        mockMvc.perform(post("/api/announcements")
                        .header("X-Username", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verify(announcementService).createAnnouncementFromDto(any(AnnouncementRequestDto.class));
        verify(announcementMapper, never()).toResponseDto(any(Announcement.class));
    }

    @Test
    @DisplayName("PUT /api/announcements/{id} - Should update announcement successfully")
    void testUpdateAnnouncement_Success() throws Exception {
        // Given
        when(announcementService.updateAnnouncement(anyLong(), any(Announcement.class))).thenReturn(announcement);
        when(announcementMapper.toResponseDto(any(Announcement.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(put("/api/announcements/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(announcement)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Recherche infirmier"));

        verify(announcementService).updateAnnouncement(eq(1L), any(Announcement.class));
        verify(announcementMapper).toResponseDto(any(Announcement.class));
    }

    @Test
    @DisplayName("PUT /api/announcements/{id} - Should return not found when announcement does not exist")
    void testUpdateAnnouncement_NotFound() throws Exception {
        // Given
        when(announcementService.updateAnnouncement(anyLong(), any(Announcement.class)))
                .thenThrow(new IllegalArgumentException("Announcement not found"));

        // When & Then
        mockMvc.perform(put("/api/announcements/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(announcement)))
                .andExpect(status().isNotFound());

        verify(announcementService).updateAnnouncement(eq(999L), any(Announcement.class));
    }

    @Test
    @DisplayName("PATCH /api/announcements/{id}/status - Should change status successfully")
    void testChangeAnnouncementStatus_Success() throws Exception {
        // Given
        announcement.setStatus(AnnouncementStatus.IN_PROGRESS);
        responseDto.setStatus(AnnouncementStatus.IN_PROGRESS);

        when(announcementService.changeAnnouncementStatus(anyLong(), any(AnnouncementStatus.class)))
                .thenReturn(announcement);
        when(announcementMapper.toResponseDto(any(Announcement.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(patch("/api/announcements/1/status")
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(announcementService).changeAnnouncementStatus(1L, AnnouncementStatus.IN_PROGRESS);
        verify(announcementMapper).toResponseDto(any(Announcement.class));
    }

    @Test
    @DisplayName("PATCH /api/announcements/{id}/status - Should return not found when announcement does not exist")
    void testChangeAnnouncementStatus_NotFound() throws Exception {
        // Given
        when(announcementService.changeAnnouncementStatus(anyLong(), any(AnnouncementStatus.class)))
                .thenThrow(new IllegalArgumentException("Announcement not found"));

        // When & Then
        mockMvc.perform(patch("/api/announcements/999/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isNotFound());

        verify(announcementService).changeAnnouncementStatus(999L, AnnouncementStatus.COMPLETED);
    }

    @Test
    @DisplayName("DELETE /api/announcements/{id} - Should delete announcement successfully")
    void testDeleteAnnouncement_Success() throws Exception {
        // Given
        doNothing().when(announcementService).deleteAnnouncement(anyLong());

        // When & Then
        mockMvc.perform(delete("/api/announcements/1"))
                .andExpect(status().isNoContent());

        verify(announcementService).deleteAnnouncement(1L);
    }

    @Test
    @DisplayName("DELETE /api/announcements/{id} - Should return not found when announcement does not exist")
    void testDeleteAnnouncement_NotFound() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Announcement not found"))
                .when(announcementService).deleteAnnouncement(anyLong());

        // When & Then
        mockMvc.perform(delete("/api/announcements/999"))
                .andExpect(status().isNotFound());

        verify(announcementService).deleteAnnouncement(999L);
    }

    @Test
    @DisplayName("GET /api/announcements/{id} - Should get announcement by id successfully")
    void testGetAnnouncementById_Success() throws Exception {
        // Given
        when(announcementService.getAnnouncementById(anyLong(), anyString())).thenReturn(announcement);
        when(announcementMapper.toResponseDto(any(Announcement.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/announcements/1")
                .header("X-Username", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Recherche infirmier"))
                .andExpect(jsonPath("$.ownerUsername").value("test"));

        verify(announcementService).getAnnouncementById(1L, "test");
        verify(announcementMapper).toResponseDto(any(Announcement.class));
    }

    @Test
    @DisplayName("GET /api/announcements/{id} - Should return not found when announcement does not exist")
    void testGetAnnouncementById_NotFound() throws Exception {
        // Given
        when(announcementService.getAnnouncementById(anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("Announcement not found"));

        // When & Then
        mockMvc.perform(get("/api/announcements/999")
                        .header("X-Username", "test"))
                .andExpect(status().isNotFound());

        verify(announcementService).getAnnouncementById(999L, "test");
        verify(announcementMapper, never()).toResponseDto(any(Announcement.class));
    }

    @Test
    @DisplayName("GET /api/announcements - Should get all announcements")
    void testGetAllAnnouncements() throws Exception {
        // Given
        List<Announcement> announcements = Arrays.asList(announcement, announcement);
        List<AnnouncementResponseDto> responseDtos = Arrays.asList(responseDto, responseDto);

        when(announcementService.getAllAnnouncementsWithPublicImages()).thenReturn(responseDtos);
        when(announcementMapper.toResponseDtoList(anyList())).thenReturn(responseDtos);

        // When & Then
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(1L));

        verify(announcementService).getAllAnnouncementsWithPublicImages();
    }

    @Test
    @DisplayName("GET /api/announcements?ownerUsername=test - Should get announcements by owner username")
    void testGetAllAnnouncements_WithOwnerUsername() throws Exception {
        // Given
        List<Announcement> announcements = List.of(announcement);
        List<AnnouncementResponseDto> responseDtos = List.of(responseDto);

        when(announcementService.getAnnouncementsByOwnerUsername("test")).thenReturn(announcements);
        when(announcementMapper.toResponseDtoList(anyList())).thenReturn(responseDtos);

        // When & Then
        mockMvc.perform(get("/api/announcements")
                        .param("ownerUsername", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ownerUsername").value("test"));

        verify(announcementService).getAnnouncementsByOwnerUsername("test");
        verify(announcementMapper).toResponseDtoList(anyList());
    }

    @Test
    @DisplayName("GET /api/announcements?status=PUBLISHED - Should get announcements by status")
    void testGetAllAnnouncements_WithStatus() throws Exception {
        // Given
        List<Announcement> announcements = List.of(announcement);
        List<AnnouncementResponseDto> responseDtos = List.of(responseDto);

        when(announcementService.getAnnouncementsByStatus(any(AnnouncementStatus.class))).thenReturn(announcements);
        when(announcementMapper.toResponseDtoList(anyList())).thenReturn(responseDtos);

        // When & Then
        mockMvc.perform(get("/api/announcements")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(announcementService).getAnnouncementsByStatus(AnnouncementStatus.PUBLISHED);
        verify(announcementMapper).toResponseDtoList(anyList());
    }

    @Test
    @DisplayName("GET /api/announcements?ownerUsername=test&status=PUBLISHED - Should get announcements by owner and status")
    void testGetAllAnnouncements_WithOwnerIdAndStatus() throws Exception {
        // Given
        List<Announcement> announcements = List.of(announcement);
        List<AnnouncementResponseDto> responseDtos = List.of(responseDto);

        when(announcementService.getAnnouncementsByOwnerUsernameAndStatus(anyString(), any(AnnouncementStatus.class)))
                .thenReturn(announcements);
        when(announcementMapper.toResponseDtoList(anyList())).thenReturn(responseDtos);

        // When & Then
        mockMvc.perform(get("/api/announcements")
                        .param("ownerUsername", "test")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ownerUsername").value("test"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(announcementService).getAnnouncementsByOwnerUsernameAndStatus("test", AnnouncementStatus.PUBLISHED);
        verify(announcementMapper).toResponseDtoList(anyList());
    }

    @Test
    @DisplayName("GET /api/announcements/owner/{ownerId} - Should get announcements by owner id")
    void testGetAnnouncementsByOwnerId() throws Exception {
        // Given
        List<Announcement> announcements = List.of(announcement);
        List<AnnouncementResponseDto> responseDtos = List.of(responseDto);

        when(announcementService.getAnnouncementsByOwnerUsername("test")).thenReturn(announcements);
        when(announcementMapper.toResponseDtoList(anyList())).thenReturn(responseDtos);

        // When & Then
        mockMvc.perform(get("/api/announcements/owner/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ownerUsername").value("test"));

        verify(announcementService).getAnnouncementsByOwnerUsername("test");
        verify(announcementMapper).toResponseDtoList(anyList());
    }

    @Test
    @DisplayName("GET /api/announcements/status/{status} - Should get announcements by status")
    void testGetAnnouncementsByStatus() throws Exception {
        // Given
        List<Announcement> announcements = List.of(announcement);
        List<AnnouncementResponseDto> responseDtos = List.of(responseDto);

        when(announcementService.getAnnouncementsByStatus(any(AnnouncementStatus.class))).thenReturn(announcements);
        when(announcementMapper.toResponseDtoList(anyList())).thenReturn(responseDtos);

        // When & Then
        mockMvc.perform(get("/api/announcements/status/PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(announcementService).getAnnouncementsByStatus(AnnouncementStatus.PUBLISHED);
        verify(announcementMapper).toResponseDtoList(anyList());
    }
}
