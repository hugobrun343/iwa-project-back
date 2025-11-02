package com.iwaproject.application.services;

import com.iwaproject.application.dtos.ApplicationRequestDto;
import com.iwaproject.application.dtos.ApplicationResponseDto;
import com.iwaproject.application.dtos.ApplicationUpdateStatusDto;
import com.iwaproject.application.entities.Application;
import com.iwaproject.application.entities.ApplicationStatus;
import com.iwaproject.application.repositories.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private AnnouncementOwnerKafkaService announcementOwnerKafkaService;

    @InjectMocks
    private ApplicationService applicationService;

    private Application testCandidature;
    private ApplicationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        testCandidature = new Application();
        testCandidature.setId(1);
        testCandidature.setAnnouncementId(100);
        testCandidature.setGuardianUsername("guardianUsername");
        testCandidature.setStatus(ApplicationStatus.SENT);
        testCandidature.setApplicationDate(LocalDateTime.now());

        requestDto = new ApplicationRequestDto();
        requestDto.setAnnouncementId(100);
        requestDto.setGuardianUsername("guardianUsername");
    }

    @Test
    void createApplication_Success() {
        when(announcementOwnerKafkaService.getAnnouncementOwner(100))
                .thenReturn(CompletableFuture.completedFuture("ownerUsername"));
        when(applicationRepository.existsByAnnouncementIdAndGuardianUsername(
                100, "guardianUsername")).thenReturn(false);
        when(applicationRepository.save(any(Application.class)))
                .thenReturn(testCandidature);

        ApplicationResponseDto result =
                applicationService.createApplicationDto(requestDto);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(100, result.getAnnouncementId());
        assertEquals("guardianUsername", result.getGuardianUsername());
        assertEquals(ApplicationStatus.SENT, result.getStatus());
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    void createApplication_AlreadyExists_ThrowsException() {
        when(announcementOwnerKafkaService.getAnnouncementOwner(100))
                .thenReturn(CompletableFuture.completedFuture("ownerUsername"));
        when(applicationRepository.existsByAnnouncementIdAndGuardianUsername(
                100, "guardianUsername")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            applicationService.createApplicationDto(requestDto);
        });

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void getApplicationById_Success() {
        when(applicationRepository.findById(1)).thenReturn(Optional.of(testCandidature));

        ApplicationResponseDto result = applicationService.getApplicationById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(100, result.getAnnouncementId());
        verify(applicationRepository, times(1)).findById(1);
    }

    @Test
    void getApplicationById_NotFound_ThrowsException() {
        when(applicationRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.getApplicationById(999);
        });
    }

    @Test
    void getAllApplications_Success() {
        List<Application> candidatures = Arrays.asList(testCandidature);
        when(applicationRepository.findAll()).thenReturn(candidatures);

        List<ApplicationResponseDto> result = applicationService.getAllApplications();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
        verify(applicationRepository, times(1)).findAll();
    }

    @Test
    void getApplicationsByAnnouncementId_Success() {
        List<Application> candidatures = Arrays.asList(testCandidature);
        when(applicationRepository.findByAnnouncementId(100)).thenReturn(candidatures);

        List<ApplicationResponseDto> result = applicationService.getApplicationsByAnnouncementId(100);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100, result.getFirst().getAnnouncementId());
        verify(applicationRepository, times(1)).findByAnnouncementId(100);
    }

    @Test
    void getApplicationsByGuardianId_Success() {
        List<Application> candidatures = Arrays.asList(testCandidature);
        when(applicationRepository.findByGuardianUsername("guardianUsername")).thenReturn(candidatures);

        List<ApplicationResponseDto> result = applicationService.getApplicationsByGuardianUsername("guardianUsername");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("guardianUsername", result.getFirst().getGuardianUsername());
        verify(applicationRepository, times(1)).findByGuardianUsername("guardianUsername");
    }

    @Test
    void getApplicationsByStatus_Success() {
        List<Application> candidatures = Arrays.asList(testCandidature);
        when(applicationRepository.findByStatus(ApplicationStatus.SENT)).thenReturn(candidatures);

        List<ApplicationResponseDto> result = applicationService.getApplicationsByStatus(ApplicationStatus.SENT);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ApplicationStatus.SENT, result.getFirst().getStatus());
        verify(applicationRepository, times(1)).findByStatus(ApplicationStatus.SENT);
    }

    @Test
    void updateApplicationStatus_Success() {
        ApplicationUpdateStatusDto updateDto = new ApplicationUpdateStatusDto();
        updateDto.setStatus(ApplicationStatus.ACCEPTED);

        when(applicationRepository.findById(1)).thenReturn(Optional.of(testCandidature));
        when(applicationRepository.save(any(Application.class))).thenReturn(testCandidature);

        ApplicationResponseDto result = applicationService.updateApplicationStatus(1, updateDto);

        assertNotNull(result);
        verify(applicationRepository, times(1)).findById(1);
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    void updateApplicationStatus_NotFound_ThrowsException() {
        ApplicationUpdateStatusDto updateDto = new ApplicationUpdateStatusDto();
        updateDto.setStatus(ApplicationStatus.ACCEPTED);

        when(applicationRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.updateApplicationStatus(999, updateDto);
        });
    }

    @Test
    void deleteApplication_Success() {
        when(applicationRepository.existsById(1)).thenReturn(true);
        doNothing().when(applicationRepository).deleteById(1);

        applicationService.deleteApplication(1);

        verify(applicationRepository, times(1)).existsById(1);
        verify(applicationRepository, times(1)).deleteById(1);
    }

    @Test
    void deleteApplication_NotFound_ThrowsException() {
        when(applicationRepository.existsById(999)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.deleteApplication(999);
        });

        verify(applicationRepository, never()).deleteById(anyInt());
    }
}
