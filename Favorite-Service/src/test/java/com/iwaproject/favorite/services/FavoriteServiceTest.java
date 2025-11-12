package com.iwaproject.favorite.services;

import com.iwaproject.favorite.dto.CreateFavoriteDTO;
import com.iwaproject.favorite.dto.FavoriteDTO;
import com.iwaproject.favorite.entities.Favorite;
import com.iwaproject.favorite.repositories.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for FavoriteService.
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    /**
     * Mock repository.
     */
    @Mock
    private FavoriteRepository favoriteRepository;

    /**
     * Service under test.
     */
    @InjectMocks
    private FavoriteService favoriteService;

    /**
     * Test constants.
     */
    private static final String TEST_USERNAME = "john";
    private static final Integer TEST_ANNOUNCEMENT_ID = 1;

    /**
     * Test favorite.
     */
    private Favorite testFavorite;

    /**
     * Setup test data.
     */
    @BeforeEach
    void setUp() {
        testFavorite = createTestFavorite();
    }

    /**
     * Test getFavoritesByGuardian when favorites exist.
     */
    @Test
    @DisplayName("getFavoritesByGuardian when favorites exist should return list")
    void getFavoritesByGuardian_favoritesExist_shouldReturnList() {
        // Given
        when(favoriteRepository.findByGuardianUsername(TEST_USERNAME))
                .thenReturn(List.of(testFavorite));

        // When
        List<FavoriteDTO> result = favoriteService.getFavoritesByGuardian(TEST_USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_USERNAME, result.get(0).getGuardianUsername());
        verify(favoriteRepository).findByGuardianUsername(TEST_USERNAME);
    }

    /**
     * Test getFavoritesByGuardian when no favorites exist.
     */
    @Test
    @DisplayName("getFavoritesByGuardian when no favorites should return empty list")
    void getFavoritesByGuardian_noFavorites_shouldReturnEmptyList() {
        // Given
        when(favoriteRepository.findByGuardianUsername(TEST_USERNAME))
                .thenReturn(List.of());

        // When
        List<FavoriteDTO> result = favoriteService.getFavoritesByGuardian(TEST_USERNAME);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(favoriteRepository).findByGuardianUsername(TEST_USERNAME);
    }

    /**
     * Test getFavoritesByAnnouncement when favorites exist.
     */
    @Test
    @DisplayName("getFavoritesByAnnouncement when favorites exist should return list")
    void getFavoritesByAnnouncement_favoritesExist_shouldReturnList() {
        // Given
        when(favoriteRepository.findByAnnouncementId(TEST_ANNOUNCEMENT_ID))
                .thenReturn(List.of(testFavorite));

        // When
        List<FavoriteDTO> result = favoriteService.getFavoritesByAnnouncement(TEST_ANNOUNCEMENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_ANNOUNCEMENT_ID, result.get(0).getAnnouncementId());
        verify(favoriteRepository).findByAnnouncementId(TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Test getFavoritesByAnnouncement when no favorites exist.
     */
    @Test
    @DisplayName("getFavoritesByAnnouncement when no favorites should return empty list")
    void getFavoritesByAnnouncement_noFavorites_shouldReturnEmptyList() {
        // Given
        when(favoriteRepository.findByAnnouncementId(TEST_ANNOUNCEMENT_ID))
                .thenReturn(List.of());

        // When
        List<FavoriteDTO> result = favoriteService.getFavoritesByAnnouncement(TEST_ANNOUNCEMENT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(favoriteRepository).findByAnnouncementId(TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Test addFavorite with valid data.
     */
    @Test
    @DisplayName("addFavorite with valid data should create favorite")
    void addFavorite_validData_shouldCreateFavorite() {
        // Given
        CreateFavoriteDTO createDTO = new CreateFavoriteDTO(TEST_ANNOUNCEMENT_ID);
        when(favoriteRepository.existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID)).thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class)))
                .thenAnswer(invocation -> {
                    Favorite saved = invocation.getArgument(0);
                    saved.setId(1);
                    return saved;
                });

        // When
        FavoriteDTO result = favoriteService.addFavorite(TEST_USERNAME, createDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.getGuardianUsername());
        assertEquals(TEST_ANNOUNCEMENT_ID, result.getAnnouncementId());
        assertNotNull(result.getDateAdded());
        verify(favoriteRepository).existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    /**
     * Test addFavorite when favorite already exists.
     */
    @Test
    @DisplayName("addFavorite when favorite exists should throw exception")
    void addFavorite_favoriteExists_shouldThrowException() {
        // Given
        CreateFavoriteDTO createDTO = new CreateFavoriteDTO(TEST_ANNOUNCEMENT_ID);
        when(favoriteRepository.existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID)).thenReturn(true);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            favoriteService.addFavorite(TEST_USERNAME, createDTO);
        });

        verify(favoriteRepository).existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
        verify(favoriteRepository, never()).save(any());
    }

    /**
     * Test removeFavorite when favorite exists.
     */
    @Test
    @DisplayName("removeFavorite when favorite exists should delete it")
    void removeFavorite_favoriteExists_shouldDeleteIt() {
        // Given
        when(favoriteRepository.existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID)).thenReturn(true);

        // When
        favoriteService.removeFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);

        // Then
        verify(favoriteRepository).existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
        verify(favoriteRepository).deleteByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Test removeFavorite when favorite does not exist.
     */
    @Test
    @DisplayName("removeFavorite when favorite not exists should throw exception")
    void removeFavorite_favoriteNotExists_shouldThrowException() {
        // Given
        when(favoriteRepository.existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID)).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            favoriteService.removeFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
        });

        verify(favoriteRepository).existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
        verify(favoriteRepository, never()).deleteByGuardianUsernameAndAnnouncementId(any(), any());
    }

    /**
     * Test isFavorite when favorite exists.
     */
    @Test
    @DisplayName("isFavorite when favorite exists should return true")
    void isFavorite_favoriteExists_shouldReturnTrue() {
        // Given
        when(favoriteRepository.existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID)).thenReturn(true);

        // When
        boolean result = favoriteService.isFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);

        // Then
        assertTrue(result);
        verify(favoriteRepository).existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Test isFavorite when favorite does not exist.
     */
    @Test
    @DisplayName("isFavorite when favorite not exists should return false")
    void isFavorite_favoriteNotExists_shouldReturnFalse() {
        // Given
        when(favoriteRepository.existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID)).thenReturn(false);

        // When
        boolean result = favoriteService.isFavorite(TEST_USERNAME, TEST_ANNOUNCEMENT_ID);

        // Then
        assertFalse(result);
        verify(favoriteRepository).existsByGuardianUsernameAndAnnouncementId(
                TEST_USERNAME, TEST_ANNOUNCEMENT_ID);
    }

    /**
     * Create a test favorite.
     *
     * @return test favorite
     */
    private Favorite createTestFavorite() {
        Favorite favorite = new Favorite();
        favorite.setId(1);
        favorite.setGuardianUsername(TEST_USERNAME);
        favorite.setAnnouncementId(TEST_ANNOUNCEMENT_ID);
        favorite.setDateAdded(LocalDateTime.now());
        return favorite;
    }
}
