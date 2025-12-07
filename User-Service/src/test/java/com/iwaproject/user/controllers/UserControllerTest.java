package com.iwaproject.user.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwaproject.user.dto.LanguageDTO;
import com.iwaproject.user.dto.PrivateUserDTO;
import com.iwaproject.user.dto.PublicUserDTO;
import com.iwaproject.user.dto.SpecialisationDTO;
import com.iwaproject.user.dto.UserLanguageDTO;
import com.iwaproject.user.dto.UserSpecialisationDTO;
import com.iwaproject.user.entities.Language;
import com.iwaproject.user.entities.Specialisation;
import com.iwaproject.user.entities.User;
import com.iwaproject.user.services.KafkaLogService;
import com.iwaproject.user.services.LanguageService;
import com.iwaproject.user.services.SpecialisationService;
import com.iwaproject.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for UserController.
 */
class UserControllerTest {

    /**
     * MockMvc for testing web layer.
     */
    private MockMvc mockMvc;

    /**
     * ObjectMapper for JSON operations.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Mock services.
     */
    @Mock
    private LanguageService languageService;
    @Mock
    private SpecialisationService specialisationService;
    @Mock
    private UserService userService;
    @Mock
    private KafkaLogService kafkaLogService;

    /**
     * Controller under test.
     */
    @InjectMocks
    private UserController userController;

    /**
     * Test constants.
     */
    private static final String X_USERNAME_HEADER = "X-Username";
    private static final String TEST_USERNAME = "john";
    private static final String TEST_EMAIL = "john@example.com";

    /**
     * Setup test environment.
     */
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    /**
     * Test GET /api/languages returns list.
     */
    @Test
    @DisplayName("GET /api/languages returns list")
    void getLanguages_ok() throws Exception {
        Language language = new Language("French");
        given(languageService.getAllLanguages()).willReturn(List.of(language));

        mockMvc.perform(get("/api/languages"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("French")));
    }

    /**
     * Test GET /api/specialisations returns list.
     */
    @Test
    @DisplayName("GET /api/specialisations returns list")
    void getSpecialisations_ok() throws Exception {
        Specialisation specialisation = new Specialisation("Plumber");
        given(specialisationService.getAllSpecialisations())
                .willReturn(List.of(specialisation));

        mockMvc.perform(get("/api/specialisations"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plumber")));
    }

    /**
     * Test GET /api/users/me with valid username returns profile.
     */
    @Test
    @DisplayName("GET /api/users/me with valid username returns profile")
    void getMyProfile_ok() throws Exception {
        User user = createTestUser();
        given(userService.getUserByUsername(TEST_USERNAME))
                .willReturn(Optional.of(user));
        given(userService.getUserEmail(TEST_USERNAME))
                .willReturn(TEST_EMAIL);

        mockMvc.perform(get("/api/users/me")
                .header(X_USERNAME_HEADER, TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.email", is(TEST_EMAIL)))
                .andExpect(jsonPath("$.firstName", is("John")));
    }

    /**
     * Test GET /api/users/me when user not found.
     */
    @Test
    @DisplayName("GET /api/users/me when user not found")
    void getMyProfile_userNotFound() throws Exception {
        given(userService.getUserByUsername(TEST_USERNAME))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                .header(X_USERNAME_HEADER, TEST_USERNAME))
                .andExpect(status().isNotFound());
    }

    /**
     * Test PATCH /api/users/me updates profile.
     */
    @Test
    @DisplayName("PATCH /api/users/me updates profile")
    void patchMyProfile_ok() throws Exception {
        User updatedUser = createTestUser();
        updatedUser.setFirstName("Johnny");
        given(userService.updateUserProfile(eq(TEST_USERNAME), any()))
                .willReturn(updatedUser);

        mockMvc.perform(patch("/api/users/me")
                .header(X_USERNAME_HEADER, TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("firstName", "Johnny"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Johnny")));
    }

    /**
     * Test PATCH /api/users/me with invalid data.
     */
    @Test
    @DisplayName("PATCH /api/users/me with invalid data")
    void patchMyProfile_invalidData() throws Exception {
        given(userService.updateUserProfile(eq(TEST_USERNAME), any()))
                .willThrow(new IllegalArgumentException("Invalid data"));

        mockMvc.perform(patch("/api/users/me")
                .header(X_USERNAME_HEADER, TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("invalidField", "value"))))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test GET /api/users/{username} returns public profile.
     */
    @Test
    @DisplayName("GET /api/users/{username} returns public profile")
    void getUserByUsername_ok() throws Exception {
        User user = createTestUser();
        given(userService.getUserByUsername("publicuser"))
                .willReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/publicuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.email").doesNotExist()); // Email not in public DTO
    }

    /**
     * Test GET /api/users/{username} when user not found.
     */
    @Test
    @DisplayName("GET /api/users/{username} when user not found")
    void getUserByUsername_userNotFound() throws Exception {
        given(userService.getUserByUsername("nonexistent"))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/users/nonexistent"))
                .andExpect(status().isNotFound());
    }

    /**
     * Test GET /api/users/me/languages returns list.
     */
    @Test
    @DisplayName("GET /api/users/me/languages returns list")
    void getMyLanguages_ok() throws Exception {
        UserLanguageDTO languageDto = new UserLanguageDTO("French");
        given(userService.getUserLanguages(TEST_USERNAME))
                .willReturn(List.of(languageDto));

        mockMvc.perform(get("/api/users/me/languages")
                .header(X_USERNAME_HEADER, TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("French")));
    }

    /**
     * Test PATCH /api/users/me/languages updates languages.
     */
    @Test
    @DisplayName("PATCH /api/users/me/languages updates languages")
    void patchMyLanguages_ok() throws Exception {
        UserLanguageDTO languageDto = new UserLanguageDTO("French");
        given(userService.updateUserLanguages(eq(TEST_USERNAME), any()))
                .willReturn(List.of(languageDto));

        mockMvc.perform(patch("/api/users/me/languages")
                .header(X_USERNAME_HEADER, TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("languages", List.of("French")))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("French")));
    }

    /**
     * Test PATCH /api/users/me/languages with missing languages field.
     */
    @Test
    @DisplayName("PATCH /api/users/me/languages with missing languages field")
    void patchMyLanguages_missingField() throws Exception {
        mockMvc.perform(patch("/api/users/me/languages")
                .header(X_USERNAME_HEADER, TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test GET /api/users/me/specialisations returns list.
     */
    @Test
    @DisplayName("GET /api/users/me/specialisations returns list")
    void getMySpecialisations_ok() throws Exception {
        UserSpecialisationDTO specialisationDto = new UserSpecialisationDTO("Plumber");
        given(userService.getUserSpecialisations(TEST_USERNAME))
                .willReturn(List.of(specialisationDto));

        mockMvc.perform(get("/api/users/me/specialisations")
                .header(X_USERNAME_HEADER, TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plumber")));
    }

    /**
     * Test PATCH /api/users/me/specialisations updates specialisations.
     */
    @Test
    @DisplayName("PATCH /api/users/me/specialisations updates specialisations")
    void patchMySpecialisations_ok() throws Exception {
        UserSpecialisationDTO specialisationDto = new UserSpecialisationDTO("Plumber");
        given(userService.updateUserSpecialisations(eq(TEST_USERNAME), any()))
                .willReturn(List.of(specialisationDto));

        mockMvc.perform(patch("/api/users/me/specialisations")
                .header(X_USERNAME_HEADER, TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("specialisations", List.of("Plumber")))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plumber")));
    }

    /**
     * Test PATCH /api/users/me/specialisations with missing specialisations field.
     */
    @Test
    @DisplayName("PATCH /api/users/me/specialisations with missing specialisations field")
    void patchMySpecialisations_missingField() throws Exception {
        mockMvc.perform(patch("/api/users/me/specialisations")
                .header(X_USERNAME_HEADER, TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    /**
     * Create a test user.
     *
     * @return test user
     */
    private User createTestUser() {
        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhoneNumber("1234567890");
        user.setLocation("Paris");
        user.setDescription("Test user");
        user.setProfilePhoto("photo.jpg"
                .getBytes(StandardCharsets.UTF_8));
        user.setIdentityVerification(false);
        user.setPreferences("{}");
        user.setRegistrationDate(LocalDateTime.now());
        return user;
    }
}