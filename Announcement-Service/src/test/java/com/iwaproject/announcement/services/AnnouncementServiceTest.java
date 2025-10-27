package com.iwaproject.announcement.services;

import com.iwaproject.announcement.entities.Announcement;
import com.iwaproject.announcement.entities.Announcement.AnnouncementStatus;
import com.iwaproject.announcement.entities.CareType;
import com.iwaproject.announcement.repositories.AnnouncementRepository;
import com.iwaproject.announcement.repositories.CareTypeRepository;
import com.iwaproject.announcement.repositories.ImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnnouncementService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementService Tests")
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private CareTypeRepository careTypeRepository;

    @Mock
    private ImageRepository imageRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    private CareType careType;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        careType = new CareType(1L, "Soins infirmiers");

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
    }

    @Test
    @DisplayName("Should create announcement successfully")
    void testCreateAnnouncement_Success() {
        // Given
        Announcement newAnnouncement = new Announcement();
        newAnnouncement.setCareType(careType);
        newAnnouncement.setTitle("Test");
        newAnnouncement.setOwnerUsername("test");

        when(careTypeRepository.findById(1L)).thenReturn(Optional.of(careType));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(announcement);

        // When
        Announcement result = announcementService.createAnnouncement(newAnnouncement);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(careTypeRepository).findById(1L);
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    @DisplayName("Should throw exception when care type not found during creation")
    void testCreateAnnouncement_CareTypeNotFound() {
        // Given
        Announcement newAnnouncement = new Announcement();
        newAnnouncement.setCareType(new CareType(999L, "Unknown"));

        when(careTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> announcementService.createAnnouncement(newAnnouncement))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Care type not found");

        verify(careTypeRepository).findById(999L);
        verify(announcementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when care type is null")
    void testCreateAnnouncement_CareTypeNull() {
        // Given
        Announcement newAnnouncement = new Announcement();
        newAnnouncement.setCareType(null);

        // When & Then
        assertThatThrownBy(() -> announcementService.createAnnouncement(newAnnouncement))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Care type is required");

        verify(announcementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update announcement successfully")
    void testUpdateAnnouncement_Success() {
        // Given
        Announcement updatedData = new Announcement();
        updatedData.setTitle("Updated Title");
        updatedData.setLocation("Lyon");
        updatedData.setDescription("Updated description");
        updatedData.setSpecificInstructions("Updated instructions");
        updatedData.setStartDate(LocalDate.now().plusDays(2));
        updatedData.setEndDate(LocalDate.now().plusDays(40));
        updatedData.setVisitFrequency("3 fois par semaine");
        updatedData.setRemuneration(60.0f);
        updatedData.setIdentityVerificationRequired(false);
        updatedData.setUrgentRequest(true);
        updatedData.setCareType(careType);

        when(announcementRepository.findById(1L)).thenReturn(Optional.of(announcement));
        when(careTypeRepository.findById(1L)).thenReturn(Optional.of(careType));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(announcement);

        // When
        Announcement result = announcementService.updateAnnouncement(1L, updatedData);

        // Then
        assertThat(result).isNotNull();
        verify(announcementRepository).findById(1L);
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    @DisplayName("Should throw exception when announcement not found during update")
    void testUpdateAnnouncement_NotFound() {
        // Given
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> announcementService.updateAnnouncement(999L, announcement))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Announcement not found");

        verify(announcementRepository).findById(999L);
        verify(announcementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should change announcement status successfully")
    void testChangeAnnouncementStatus_Success() {
        // Given
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(announcement));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(announcement);

        // When
        Announcement result = announcementService.changeAnnouncementStatus(1L, AnnouncementStatus.IN_PROGRESS);

        // Then
        assertThat(result).isNotNull();
        verify(announcementRepository).findById(1L);
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    @DisplayName("Should throw exception when announcement not found during status change")
    void testChangeAnnouncementStatus_NotFound() {
        // Given
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> announcementService.changeAnnouncementStatus(999L, AnnouncementStatus.COMPLETED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Announcement not found");

        verify(announcementRepository).findById(999L);
        verify(announcementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete announcement successfully")
    void testDeleteAnnouncement_Success() {
        // Given
        when(announcementRepository.existsById(1L)).thenReturn(true);
        doNothing().when(announcementRepository).deleteById(1L);

        // When
        announcementService.deleteAnnouncement(1L);

        // Then
        verify(announcementRepository).existsById(1L);
        verify(announcementRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when announcement not found during deletion")
    void testDeleteAnnouncement_NotFound() {
        // Given
        when(announcementRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> announcementService.deleteAnnouncement(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Announcement not found");

        verify(announcementRepository).existsById(999L);
        verify(announcementRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should get announcement by id")
    void testGetAnnouncementById_Success() {
        // Given
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(announcement));

        // When
        Announcement result = announcementService.getAnnouncementById(1L, "test");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(announcementRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when announcement not found by id")
    void testGetAnnouncementById_NotFound() {
        // Given
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        assertThatThrownBy(() -> announcementService.getAnnouncementById(999L, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Announcement not found with id: 999");
    }

    @Test
    @DisplayName("Should get all announcements")
    void testGetAllAnnouncements() {
        // Given
        List<Announcement> announcements = Arrays.asList(announcement, announcement);
        when(announcementRepository.findAll()).thenReturn(announcements);

        // When
        List<Announcement> result = announcementService.getAllAnnouncements();

        // Then
        assertThat(result).hasSize(2);
        verify(announcementRepository).findAll();
    }

    @Test
    @DisplayName("Should get announcements by owner id")
    void testGetAnnouncementsByOwnerId() {
        // Given
        List<Announcement> announcements = List.of(announcement);
        when(announcementRepository.findByOwnerUsername("test")).thenReturn(announcements);

        // When
        List<Announcement> result = announcementService.getAnnouncementsByOwnerUsername("test");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getOwnerUsername()).isEqualTo("test");
        verify(announcementRepository).findByOwnerUsername("test");
    }

    @Test
    @DisplayName("Should get announcements by status")
    void testGetAnnouncementsByStatus() {
        // Given
        List<Announcement> announcements = List.of(announcement);
        when(announcementRepository.findByStatus(AnnouncementStatus.PUBLISHED)).thenReturn(announcements);

        // When
        List<Announcement> result = announcementService.getAnnouncementsByStatus(AnnouncementStatus.PUBLISHED);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
        verify(announcementRepository).findByStatus(AnnouncementStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Should get announcements by owner id and status")
    void testGetAnnouncementsByOwnerIdAndStatus() {
        // Given
        List<Announcement> announcements = List.of(announcement);
        when(announcementRepository.findByOwnerUsernameAndStatus("test", AnnouncementStatus.PUBLISHED))
                .thenReturn(announcements);

        // When
        List<Announcement> result = announcementService.getAnnouncementsByOwnerUsernameAndStatus("test", AnnouncementStatus.PUBLISHED);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getOwnerUsername()).isEqualTo("test");
        assertThat(result.getFirst().getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
        verify(announcementRepository).findByOwnerUsernameAndStatus("test", AnnouncementStatus.PUBLISHED);
    }
}
