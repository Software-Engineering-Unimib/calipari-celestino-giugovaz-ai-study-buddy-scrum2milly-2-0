package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.UserRepository;
import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.util.enums.EducationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private UUID userId;
    private User user;
    private String email;
    private String password;
    private String encodedPassword;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "mario.rossi@example.com";
        password = "Password123!";
        encodedPassword = "encodedPassword123";

        user = new User();
        user.setId(userId);
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail(email);
        user.setPasswordHash(encodedPassword);
        user.setEducationLevel(EducationLevel.UNIVERSITY);
        user.setTotalPoints(150);
        user.setLevel(2);
        user.setStreakDays(5);
        user.setAvatarUrl("https://example.com/avatar.jpg");
        user.setPreferredLanguage("it");
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
        user.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    // ==================== CRUD TESTS ====================

    @Test
    void getAllUsers_Success() {
        // Arrange
        List<User> expectedUsers = Arrays.asList(user, new User());
        when(userRepository.findAll()).thenReturn(expectedUsers);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedUsers);
        verify(userRepository).findAll();
    }

    @Test
    void findById_Success() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findById(userId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId);
        assertThat(result.get().getEmail()).isEqualTo(email);
        verify(userRepository).findById(userId);
    }

    @Test
    void findById_NotFound_ReturnsEmpty() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findById(userId);

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findById(userId);
    }

    @Test
    void findByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByEmail(email);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void save_Success() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        User result = userService.save(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void deleteById_Success() {
        // Arrange
        doNothing().when(userRepository).deleteById(userId);

        // Act
        userService.deleteById(userId);

        // Assert
        verify(userRepository).deleteById(userId);
    }

    // ==================== USER DETAILS SERVICE TESTS ====================

    @Test
    void loadUserByUsername_Success() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo(encodedPassword);
        assertThat(userDetails.getAuthorities()).isEmpty();
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(Const.USER_NOT_FOUND);

        verify(userRepository).findByEmail(email);
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    void registerUser_Success() {
        // Arrange
        String firstName = "Mario";
        String lastName = "Rossi";
        EducationLevel educationLevel = EducationLevel.UNIVERSITY;

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.registerUser(firstName, lastName, email, password, educationLevel);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(firstName);
        assertThat(result.getLastName()).isEqualTo(lastName);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getPasswordHash()).isEqualTo(encodedPassword);
        assertThat(result.getEducationLevel()).isEqualTo(educationLevel);
        assertThat(result.getTotalPoints()).isEqualTo(0);
        assertThat(result.getLevel()).isEqualTo(1);
        assertThat(result.getStreakDays()).isZero();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_EmailAlreadyExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser("Mario", "Rossi", email, password, EducationLevel.UNIVERSITY))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining(Const.EMAIL_EXISTS);

        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void existsByEmail_ReturnsTrue() {
        // Arrange
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act
        boolean result = userService.existsByEmail(email);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void existsByEmail_ReturnsFalse() {
        // Arrange
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // Act
        boolean result = userService.existsByEmail(email);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(email);
    }

    // ==================== CURRENT USER TESTS ====================

    @Test
    void getCurrentUser_Success() {
        // Arrange
        when(principal.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        User result = userService.getCurrentUser(principal);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        verify(principal).getName();
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getCurrentUser_NotFound_ThrowsException() {
        // Arrange
        when(principal.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getCurrentUser(principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED)
                .hasMessageContaining(Const.UNAUTHORIZED);

        verify(principal).getName();
        verify(userRepository).findByEmail(email);
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Test
    void updateProfile_AllFields_Success() {
        // Arrange
        String newFirstName = "Luigi";
        String newLastName = "Verdi";
        String newAvatarUrl = "https://example.com/new-avatar.jpg";

        when(principal.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.updateProfile(principal, newFirstName, newLastName, newAvatarUrl);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(newFirstName);
        assertThat(result.getLastName()).isEqualTo(newLastName);
        assertThat(result.getAvatarUrl()).isEqualTo(newAvatarUrl);
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_PartialFields_Success() {
        // Arrange
        String newFirstName = "Luigi";
        String newLastName = null;
        String newAvatarUrl = null;

        when(principal.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.updateProfile(principal, newFirstName, newLastName, newAvatarUrl);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(newFirstName);
        assertThat(result.getLastName()).isEqualTo("Rossi"); // Invariato
        assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg"); // Invariato
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_BlankFields_Ignored() {
        // Arrange
        String newFirstName = "  "; // Blank
        String newLastName = "  "; // Blank
        String newAvatarUrl = null;

        when(principal.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.updateProfile(principal, newFirstName, newLastName, newAvatarUrl);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Mario"); // Invariato
        assertThat(result.getLastName()).isEqualTo("Rossi"); // Invariato
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_UserNotFound_ThrowsException() {
        // Arrange
        when(principal.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateProfile(principal, "Luigi", "Verdi", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(userRepository, never()).save(any());
    }

    // ==================== POINTS TESTS ====================

    @Test
    void addPoints_Success() {
        // Arrange
        int initialPoints = user.getTotalPoints();
        int initialLevel = user.getLevel();
        int pointsToAdd = 50;

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.addPoints(userId, pointsToAdd);

        // Assert
        assertThat(user.getTotalPoints()).isEqualTo(initialPoints + pointsToAdd);
        assertThat(user.getLevel()).isEqualTo(initialLevel + 1); // 150+50=200 → level 3
        assertThat(user.getUpdatedAt()).isNotNull();

        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void addPoints_NoLevelUp_Success() {
        // Arrange
        int initialPoints = user.getTotalPoints();
        int pointsToAdd = 20; // 150+20=170 → level 2 (invariato)

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.addPoints(userId, pointsToAdd);

        // Assert
        assertThat(user.getTotalPoints()).isEqualTo(initialPoints + pointsToAdd);
        assertThat(user.getLevel()).isEqualTo(2); // Invariato
        verify(userRepository).save(user);
    }

    @Test
    void addPoints_MultipleLevelUps_Success() {
        // Arrange
        user.setTotalPoints(250); // Level 3 (250/100 + 1 = 3)
        user.setLevel(3);
        int pointsToAdd = 150; // 250+150=400 → level 5 (400/100 + 1 = 5)

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.addPoints(userId, pointsToAdd);

        // Assert
        assertThat(user.getTotalPoints()).isEqualTo(400);
        assertThat(user.getLevel()).isEqualTo(5);
        verify(userRepository).save(user);
    }

    @Test
    void addPoints_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.addPoints(userId, 50))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining(Const.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    // ==================== STREAK TESTS ====================

    @Test
    void updateStreak_Success() {
        // Arrange
        int initialStreak = user.getStreakDays();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.updateStreak(userId);

        // Assert
        assertThat(user.getStreakDays()).isEqualTo(initialStreak + 1);
        assertThat(user.getUpdatedAt()).isNotNull();
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void updateStreak_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateStreak(userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining(Const.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetStreak_Success() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.resetStreak(userId);

        // Assert
        assertThat(user.getStreakDays()).isZero();
        assertThat(user.getUpdatedAt()).isNotNull();
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void resetStreak_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.resetStreak(userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining(Const.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    // ==================== EDGE CASES TESTS ====================

    @Test
    void addPoints_ZeroPoints_NoChange() {
        // Arrange
        int initialPoints = user.getTotalPoints();
        int initialLevel = user.getLevel();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.addPoints(userId, 0);

        // Assert
        assertThat(user.getTotalPoints()).isEqualTo(initialPoints);
        assertThat(user.getLevel()).isEqualTo(initialLevel);
        verify(userRepository).save(user);
    }

    @Test
    void addPoints_NegativePoints_DecreasesTotalAndLevel() {
        // Arrange
        int initialPoints = user.getTotalPoints();
        int initialLevel = user.getLevel();
        int pointsToSubtract = -30; // 150-30=120 → level 2 (invariato)

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.addPoints(userId, pointsToSubtract);

        // Assert
        assertThat(user.getTotalPoints()).isEqualTo(initialPoints + pointsToSubtract);
        assertThat(user.getLevel()).isEqualTo(initialLevel); // Livello invariato
        verify(userRepository).save(user);
    }

    @Test
    void addPoints_NegativePoints_LevelDown() {
        // Arrange
        user.setTotalPoints(250); // Level 3
        user.setLevel(3);
        int pointsToSubtract = -80; // 250-80=170 → level 2

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.addPoints(userId, pointsToSubtract);

        // Assert
        assertThat(user.getTotalPoints()).isEqualTo(170);
        assertThat(user.getLevel()).isEqualTo(2);
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_WithNullAvatar_Success() {
        // Arrange
        String newFirstName = "Luigi";
        String newLastName = "Verdi";
        String newAvatarUrl = null;

        when(principal.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.updateProfile(principal, newFirstName, newLastName, newAvatarUrl);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(newFirstName);
        assertThat(result.getLastName()).isEqualTo(newLastName);
        assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg"); // Invariato
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_WithEmptyAvatar_Success() {
        // Arrange
        String newFirstName = "Luigi";
        String newLastName = "Verdi";
        String newAvatarUrl = "";

        when(principal.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.updateProfile(principal, newFirstName, newLastName, newAvatarUrl);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(newFirstName);
        assertThat(result.getLastName()).isEqualTo(newLastName);
        assertThat(result.getAvatarUrl()).isEqualTo(""); // Aggiornato con stringa vuota
        verify(userRepository).save(user);
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    void registerUser_WithNullEducationLevel_Success() {
        // Arrange
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.registerUser("Mario", "Rossi", email, password, (EducationLevel) null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEducationLevel()).isNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WithMinimalFields_Success() {
        // Arrange
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.registerUser("M", "R", email, "pwd", EducationLevel.HIGH_SCHOOL);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("M");
        assertThat(result.getLastName()).isEqualTo("R");
        verify(userRepository).save(any(User.class));
    }

    // ==================== INTEGRATION POINTS TESTS ====================

    @Test
    void addPoints_UpdatesUpdatedAt_Success() {
        // Arrange
        LocalDateTime oldUpdatedAt = user.getUpdatedAt();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.addPoints(userId, 10);

        // Assert
        assertThat(user.getUpdatedAt()).isAfter(oldUpdatedAt);
        verify(userRepository).save(user);
    }

    @Test
    void updateStreak_UpdatesUpdatedAt_Success() {
        // Arrange
        LocalDateTime oldUpdatedAt = user.getUpdatedAt();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.updateStreak(userId);

        // Assert
        assertThat(user.getUpdatedAt()).isAfter(oldUpdatedAt);
        verify(userRepository).save(user);
    }

    @Test
    void resetStreak_UpdatesUpdatedAt_Success() {
        // Arrange
        LocalDateTime oldUpdatedAt = user.getUpdatedAt();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.resetStreak(userId);

        // Assert
        assertThat(user.getUpdatedAt()).isAfter(oldUpdatedAt);
        verify(userRepository).save(user);
    }

    // ==================== PRINCIPAL NULL TESTS ====================

    @Test
    void getCurrentUser_NullPrincipal_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.getCurrentUser(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateProfile_NullPrincipal_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.updateProfile(null, "Luigi", "Verdi", null))
                .isInstanceOf(NullPointerException.class);
    }
}