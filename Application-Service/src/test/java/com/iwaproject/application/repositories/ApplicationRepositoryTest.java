package com.iwaproject.application.repositories;

import com.iwaproject.application.entities.Application;
import com.iwaproject.application.entities.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    private Application testCandidature1;
    private Application testCandidature2;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();

        testCandidature1 = new Application();
        testCandidature1.setAnnouncementId(100);
        testCandidature1.setGuardianUsername("guardianUsername");
        testCandidature1.setStatus(ApplicationStatus.SENT);
        testCandidature1.setApplicationDate(LocalDateTime.now());

        testCandidature2 = new Application();
        testCandidature2.setAnnouncementId(101);
        testCandidature2.setGuardianUsername("guardianUsername");
        testCandidature2.setStatus(ApplicationStatus.ACCEPTED);
        testCandidature2.setApplicationDate(LocalDateTime.now());

        applicationRepository.save(testCandidature1);
        applicationRepository.save(testCandidature2);
    }

    @Test
    void findByAnnouncementId_Success() {
        List<Application> candidatures = applicationRepository.findByAnnouncementId(100);

        assertNotNull(candidatures);
        assertEquals(1, candidatures.size());
        assertEquals(100, candidatures.get(0).getAnnouncementId());
    }

    @Test
    void findByGuardianId_Success() {
        List<Application> candidatures = applicationRepository.findByGuardianUsername("guardianUsername");

        assertNotNull(candidatures);
        assertEquals(2, candidatures.size());
    }

    @Test
    void findByStatus_Success() {
        List<Application> candidatures = applicationRepository.findByStatus(ApplicationStatus.SENT);

        assertNotNull(candidatures);
        assertEquals(1, candidatures.size());
        assertEquals(ApplicationStatus.SENT, candidatures.get(0).getStatus());
    }

    @Test
    void findByAnnouncementIdAndStatus_Success() {
        List<Application> candidatures = applicationRepository
                .findByAnnouncementIdAndStatus(100, ApplicationStatus.SENT);

        assertNotNull(candidatures);
        assertEquals(1, candidatures.size());
        assertEquals(100, candidatures.getFirst().getAnnouncementId());
        assertEquals(ApplicationStatus.SENT, candidatures.getFirst().getStatus());
    }

    @Test
    void findByGuardianIdAndStatus_Success() {
        List<Application> candidatures = applicationRepository
                .findByGuardianUsernameAndStatus("guardianUsername", ApplicationStatus.ACCEPTED);

        assertNotNull(candidatures);
        assertEquals(1, candidatures.size());
        assertEquals("guardianUsername", candidatures.getFirst().getGuardianUsername());
        assertEquals(ApplicationStatus.ACCEPTED, candidatures.getFirst().getStatus());
    }

    @Test
    void existsByAnnouncementIdAndGuardianId_True() {
        boolean exists = applicationRepository.existsByAnnouncementIdAndGuardianUsername(100, "guardianUsername");

        assertTrue(exists);
    }

    @Test
    void existsByAnnouncementIdAndGuardianId_False() {
        boolean exists = applicationRepository.existsByAnnouncementIdAndGuardianUsername(999, "nonExistentUser");

        assertFalse(exists);
    }
}
