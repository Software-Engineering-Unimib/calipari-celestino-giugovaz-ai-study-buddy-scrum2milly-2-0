package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.UserRepository;
import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.util.enums.EducationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - Test Suite Completo")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UUID testUserId;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ENCODED_PASSWORD = "encoded_password";
    private static final String TEST_FIRST_NAME = "Mario";
    private static final String TEST_LAST_NAME = "Rossi";

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = createTestUser();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(testUserId);
        user.setFirstName(TEST_FIRST_NAME);
        user.setLastName(TEST_LAST_NAME);
        user.setEmail(TEST_EMAIL);
        user.setPasswordHash(TEST_ENCODED_PASSWORD);
        user.setEducationLevel(EducationLevel.UNIVERSITY);
        user.setPreferredLanguage("it");
        user.setTotalPoints(0);
        user.setLevel(1);
        user.setStreakDays(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    // ========================================
    // TEST: CRUD BASE
    // ========================================

    @Test
    @DisplayName("getAllUsers - Restituisce lista utenti")
    void testGetAllUsers_Success() {
        // Arrange
        List<User> users = Arrays.asList(testUser, createTestUser());
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findById - Trova utente esistente")
    void testFindById_UserExists() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findById(testUserId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_EMAIL, result.get().getEmail());
        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    @DisplayName("findById - Utente non trovato")
    void testFindById_UserNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findById(nonExistentId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findByEmail - Trova utente per email")
    void testFindByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByEmail(TEST_EMAIL);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_EMAIL, result.get().getEmail());
    }

    @Test
    @DisplayName("save - Salva utente e aggiorna updatedAt")
    void testSave_Success() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.save(testUser);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getUpdatedAt());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("deleteById - Elimina utente")
    void testDeleteById_Success() {
        // Arrange
        doNothing().when(userRepository).deleteById(testUserId);

        // Act
        userService.deleteById(testUserId);

        // Assert
        verify(userRepository, times(1)).deleteById(testUserId);
    }

    // ========================================
    // TEST: REGISTRAZIONE
    // ========================================

    @Test
    @DisplayName("registerUser - Registrazione completa con education level e lingua")
    void testRegisterUser_FullRegistration() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.registerUser(
            TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, TEST_PASSWORD,
            EducationLevel.UNIVERSITY, "en"
        );

        // Assert
        assertNotNull(result);
        assertEquals(TEST_FIRST_NAME, result.getFirstName());
        assertEquals(TEST_LAST_NAME, result.getLastName());
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_ENCODED_PASSWORD, result.getPasswordHash());
        assertEquals(EducationLevel.UNIVERSITY, result.getEducationLevel());
        assertEquals("en", result.getPreferredLanguage());
        assertEquals(1, result.getLevel());
        assertEquals(0, result.getTotalPoints());
        assertEquals(0, result.getStreakDays());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("registerUser - Solo education level, lingua default")
    void testRegisterUser_OnlyEducationLevel() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.registerUser(
            TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, TEST_PASSWORD,
            EducationLevel.HIGH_SCHOOL
        );

        // Assert
        assertNotNull(result);
        assertEquals(EducationLevel.HIGH_SCHOOL, result.getEducationLevel());
        assertEquals("it", result.getPreferredLanguage()); // Default
    }

    @Test
    @DisplayName("registerUser - Solo lingua preferita")
    void testRegisterUser_OnlyLanguage() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.registerUser(
            TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, TEST_PASSWORD, "es"
        );

        // Assert
        assertNotNull(result);
        assertNull(result.getEducationLevel());
        assertEquals("es", result.getPreferredLanguage());
    }

    @Test
    @DisplayName("registerUser - Email già esistente lancia eccezione")
    void testRegisterUser_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.registerUser(
                TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, TEST_PASSWORD,
                EducationLevel.UNIVERSITY, "it"
            );
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains(Const.EMAIL_EXISTS));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("registerUser - Lingua non supportata lancia eccezione")
    void testRegisterUser_UnsupportedLanguage() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.registerUser(
                TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, TEST_PASSWORD,
                EducationLevel.UNIVERSITY, "xyz"
            );
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Lingua non supportata"));
    }

    // ========================================
    // TEST: AUTENTICAZIONE
    // ========================================

    @Test
    @DisplayName("loadUserByUsername - Carica utente per email")
    void testLoadUserByUsername_Success() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = userService.loadUserByUsername(TEST_EMAIL);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getUsername());
        assertEquals(TEST_ENCODED_PASSWORD, result.getPassword());
    }

    @Test
    @DisplayName("loadUserByUsername - Utente non trovato lancia eccezione")
    void testLoadUserByUsername_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername(TEST_EMAIL);
        });
    }

    @Test
    @DisplayName("existsByEmail - Restituisce true se email esiste")
    void testExistsByEmail_True() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // Act
        boolean result = userService.existsByEmail(TEST_EMAIL);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("existsByEmail - Restituisce false se email non esiste")
    void testExistsByEmail_False() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // Act
        boolean result = userService.existsByEmail(TEST_EMAIL);

        // Assert
        assertFalse(result);
    }

    // ========================================
    // TEST: PROFILO UTENTE
    // ========================================

    @Test
    @DisplayName("getCurrentUser - Restituisce utente corrente")
    void testGetCurrentUser_Success() {
        // Arrange
        when(principal.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getCurrentUser(principal);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
    }

    @Test
    @DisplayName("getCurrentUser - Utente non autenticato lancia eccezione")
    void testGetCurrentUser_Unauthorized() {
        // Arrange
        when(principal.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.getCurrentUser(principal);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    @DisplayName("updateProfile - Aggiorna nome e cognome")
    void testUpdateProfile_UpdateNameAndLastName() {
        // Arrange
        String newFirstName = "Luigi";
        String newLastName = "Verdi";
        when(principal.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.updateProfile(principal, newFirstName, newLastName, null);

        // Assert
        assertEquals(newFirstName, result.getFirstName());
        assertEquals(newLastName, result.getLastName());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("updateProfile - Aggiorna avatar URL")
    void testUpdateProfile_UpdateAvatarUrl() {
        // Arrange
        String newAvatarUrl = "https://example.com/avatar.png";
        when(principal.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.updateProfile(principal, null, null, newAvatarUrl);

        // Assert
        assertEquals(newAvatarUrl, result.getAvatarUrl());
    }

    @Test
    @DisplayName("updateProfile - Ignora valori blank")
    void testUpdateProfile_IgnoreBlankValues() {
        // Arrange
        String originalFirstName = testUser.getFirstName();
        when(principal.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.updateProfile(principal, "", null, null);

        // Assert
        assertEquals(originalFirstName, result.getFirstName()); // Non cambiato
    }

    // ========================================
    // TEST: GAMIFICATION
    // ========================================

    @Test
    @DisplayName("addPoints - Aggiunge punti e calcola livello")
    void testAddPoints_Success() {
        // Arrange
        int pointsToAdd = 150;
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.addPoints(testUserId, pointsToAdd);

        // Assert
        assertEquals(150, testUser.getTotalPoints());
        assertEquals(2, testUser.getLevel()); // 150 / 100 + 1 = 2
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("addPoints - Calcolo corretto livello con molti punti")
    void testAddPoints_HighLevel() {
        // Arrange
        testUser.setTotalPoints(500); // Livello iniziale 6
        int pointsToAdd = 250;
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.addPoints(testUserId, pointsToAdd);

        // Assert
        assertEquals(750, testUser.getTotalPoints());
        assertEquals(8, testUser.getLevel()); // 750 / 100 + 1 = 8
    }

    @Test
    @DisplayName("addPoints - Utente non trovato lancia eccezione")
    void testAddPoints_UserNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.addPoints(nonExistentId, 100);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("updateStreak - Incrementa streak days")
    void testUpdateStreak_Success() {
        // Arrange
        testUser.setStreakDays(5);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.updateStreak(testUserId);

        // Assert
        assertEquals(6, testUser.getStreakDays());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("resetStreak - Resetta streak a zero")
    void testResetStreak_Success() {
        // Arrange
        testUser.setStreakDays(10);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.resetStreak(testUserId);

        // Assert
        assertEquals(0, testUser.getStreakDays());
        verify(userRepository, times(1)).save(testUser);
    }

    // ========================================
    // TEST: LINGUA PREFERITA
    // ========================================

    @Test
    @DisplayName("updatePreferredLanguage - Aggiorna lingua con successo")
    void testUpdatePreferredLanguage_Success() {
        // Arrange
        String newLanguage = "en";
        testUser.setPreferredLanguage("it");
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.updatePreferredLanguage(testUserId, newLanguage);

        // Assert
        assertEquals(newLanguage, result.getPreferredLanguage());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("updatePreferredLanguage - Lingua non supportata lancia eccezione")
    void testUpdatePreferredLanguage_UnsupportedLanguage() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.updatePreferredLanguage(testUserId, "invalid");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("non supportata"));
    }

    @Test
    @DisplayName("updatePreferredLanguage - Supporta tutte le lingue valide")
    void testUpdatePreferredLanguage_AllSupportedLanguages() {
        // Arrange
        List<String> supportedLanguages = Arrays.asList("it", "en", "es", "fr", "de", "pt", "ru");
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        for (String lang : supportedLanguages) {
            User result = userService.updatePreferredLanguage(testUserId, lang);
            assertEquals(lang, result.getPreferredLanguage());
        }
    }

    @Test
    @DisplayName("User.getFullName - Restituisce nome completo")
    void testGetFullName() {
        // Act
        String fullName = testUser.getFullName();

        // Assert
        assertEquals("Mario Rossi", fullName);
    }
}
