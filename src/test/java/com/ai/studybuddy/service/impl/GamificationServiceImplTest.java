package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.gamification.GamificationDTO.*;
import com.ai.studybuddy.model.gamification.*;
import com.ai.studybuddy.model.gamification.Recommendation.Priority;
import com.ai.studybuddy.model.gamification.Recommendation.RecommendationType;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.model.user.UserProgress;
import com.ai.studybuddy.repository.*;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceImplTest {

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProgressRepository userProgressRepository;

    @InjectMocks
    private GamificationServiceImpl gamificationService;

    @Captor
    private ArgumentCaptor<UserStats> userStatsCaptor;

    @Captor
    private ArgumentCaptor<UserProgress> userProgressCaptor;

    @Captor
    private ArgumentCaptor<UserBadge> userBadgeCaptor;

    @Captor
    private ArgumentCaptor<Recommendation> recommendationCaptor;

    private UUID userId;
    private UUID badgeId;
    private UUID recommendationId;
    private User user;
    private UserStats userStats;
    private Badge badge;
    private UserBadge userBadge;
    private Recommendation recommendation;
    private UserProgress userProgress;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        badgeId = UUID.randomUUID();
        recommendationId = UUID.randomUUID();

        // Setup User
        user = new User();
        user.setId(userId);
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");

        // Setup UserStats
        userStats = new UserStats();
        userStats.setId(UUID.randomUUID());
        userStats.setUser(user);
        userStats.setTotalXp(500);
        userStats.setLevel(2);
        userStats.setCurrentStreak(5);
        userStats.setExplanationsRequested(10);
        userStats.setQuizzesCompleted(15);
        userStats.setQuizzesPassed(12);
        userStats.setFlashcardsStudied(50);
        userStats.setFocusSessionsCompleted(3);
        userStats.setTotalStudyTimeMinutes(120);
        userStats.setWeeklyXp(30);
        userStats.setLastActivityDate(LocalDate.now().minusDays(1));

        // Setup Badge
        badge = new Badge();
        badge.setId(badgeId);
        badge.setName("Esperto Quiz");
        badge.setDescription("Completa 20 quiz");
        badge.setRequirementType("QUIZZES_COMPLETED");
        badge.setRequirementValue(20);
        badge.setXpReward(50);
        badge.setIsActive(true);

        // Setup UserBadge
        userBadge = new UserBadge();
        userBadge.setId(UUID.randomUUID());
        userBadge.setUser(user);
        userBadge.setBadge(badge);
        userBadge.setUnlockedAt(LocalDateTime.now());
        userBadge.setProgressAtUnlock(20);
        userBadge.setIsNew(true);

        // Setup Recommendation
        recommendation = new Recommendation();
        recommendation.setId(recommendationId);
        recommendation.setUser(user);
        recommendation.setType(RecommendationType.WEAKNESS_FOCUS);
        recommendation.setTitle("Ripassa Matematica");
        recommendation.setDescription("Il tuo punteggio è basso");
        recommendation.setTopic("Algebra");
        recommendation.setReason("Punteggio sotto il 60%");
        recommendation.setPriority(Priority.HIGH);
        recommendation.setCreatedAt(LocalDateTime.now());
        recommendation.setExpiresAt(LocalDateTime.now().plusDays(1));
        recommendation.setIsDismissed(false);
        recommendation.setIsCompleted(false);

        // Setup UserProgress
        userProgress = new UserProgress();
        userProgress.setUser(user);
        userProgress.setTopic("Algebra");
        userProgress.setSubject("Mathematics");
        userProgress.setQuizCompleted(3);
        userProgress.setTotalQuestions(30);
        userProgress.setCorrectAnswers(15);
        userProgress.setAverageScore(50.0);
        userProgress.setMasteryLevel(DifficultyLevel.PRINCIPIANTE);
        userProgress.setLastActivityAt(LocalDateTime.now().minusDays(2));
    }

    // ==================== USER STATS TESTS ====================

    @Test
    void getOrCreateUserStats_ExistingStats_Success() {
        // Arrange
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));

        // Act
        UserStats result = gamificationService.getOrCreateUserStats(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userStats.getId());
        assertThat(result.getUser()).isEqualTo(user);
        verify(userStatsRepository).findByUserId(userId);
        verify(userRepository, never()).findById(any());
        verify(userStatsRepository, never()).save(any());
    }

    @Test
    void getOrCreateUserStats_NewStats_Success() {
        // Arrange
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserStats result = gamificationService.getOrCreateUserStats(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getTotalXp()).isZero();
        assertThat(result.getLevel()).isEqualTo(1);
        assertThat(result.getCurrentStreak()).isZero();
        verify(userStatsRepository).findByUserId(userId);
        verify(userRepository).findById(userId);
        verify(userStatsRepository).save(any(UserStats.class));
    }

    @Test
    void getUserStatsResponse_Success() {
        // Arrange
        long badgeCount = 3L;
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userBadgeRepository.countByUserId(userId)).thenReturn(badgeCount);

        // Act
        UserStatsResponse response = gamificationService.getUserStatsResponse(userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTotalXp()).isEqualTo(userStats.getTotalXp());
        assertThat(response.getLevel()).isEqualTo(userStats.getLevel());
        assertThat(response.getCurrentStreak()).isEqualTo(userStats.getCurrentStreak());
        assertThat(response.getBadgesUnlocked()).isEqualTo(badgeCount);
    }

    // ==================== XP EVENTS TESTS ====================

    @Test
    void recordExplanationXp_Success() {
        // Arrange
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(userStats);
        when(badgeRepository.findUnlockableBadges(anyString(), anyInt())).thenReturn(new ArrayList<>());

        // Act
        XpEventResponse response = gamificationService.recordExplanationXp(user);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getXpEarned()).isEqualTo(10);
        assertThat(response.isLeveledUp()).isFalse();
        assertThat(response.getNewBadges()).isEmpty();

        verify(userStatsRepository).save(userStatsCaptor.capture());
        UserStats savedStats = userStatsCaptor.getValue();
        assertThat(savedStats.getExplanationsRequested()).isEqualTo(11);
        assertThat(savedStats.getTotalXp()).isEqualTo(510);
    }

    @Test
    void recordExplanationXp_WithTopic_Success() {
        // Arrange
        String topic = "Algebra";
        String subject = "Mathematics";

        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(userStats);
        when(badgeRepository.findUnlockableBadges(anyString(), anyInt())).thenReturn(new ArrayList<>());
        when(userProgressRepository.findByUserIdAndTopic(userId, topic)).thenReturn(Optional.empty());
        when(userProgressRepository.save(any(UserProgress.class))).thenReturn(userProgress);

        // Act
        XpEventResponse response = gamificationService.recordExplanationXp(user, topic, subject);

        // Assert
        assertThat(response).isNotNull();
        verify(userProgressRepository).save(userProgressCaptor.capture());
        UserProgress savedProgress = userProgressCaptor.getValue();
        assertThat(savedProgress.getTopic()).isEqualTo(topic);
        assertThat(savedProgress.getSubject()).isEqualTo(subject);
    }

    @Test
    void recordQuizXp_Passed_Success() {
        // Arrange
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(userStats);
        when(badgeRepository.findUnlockableBadges(anyString(), anyInt())).thenReturn(new ArrayList<>());

        // Act
        XpEventResponse response = gamificationService.recordQuizXp(user, true);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getXpEarned()).isEqualTo(30); // 20 + 10 bonus
        assertThat(response.getEventType()).isEqualTo("QUIZ");

        verify(userStatsRepository).save(userStatsCaptor.capture());
        UserStats savedStats = userStatsCaptor.getValue();
        assertThat(savedStats.getQuizzesCompleted()).isEqualTo(16);
        assertThat(savedStats.getQuizzesPassed()).isEqualTo(13);
        assertThat(savedStats.getTotalXp()).isEqualTo(530);
    }

    @Test
    void recordQuizXp_Failed_Success() {
        // Arrange
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(userStats);
        when(badgeRepository.findUnlockableBadges(anyString(), anyInt())).thenReturn(new ArrayList<>());

        // Act
        XpEventResponse response = gamificationService.recordQuizXp(user, false);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getXpEarned()).isEqualTo(20); // Solo 20, nessun bonus
        verify(userStatsRepository).save(userStatsCaptor.capture());
        UserStats savedStats = userStatsCaptor.getValue();
        assertThat(savedStats.getQuizzesCompleted()).isEqualTo(16);
        assertThat(savedStats.getQuizzesPassed()).isEqualTo(12); // Invariato
    }

    @Test
    void recordQuizXp_WithDetails_Success() {
        // Arrange
        String topic = "Algebra";
        String subject = "Mathematics";
        double score = 70.0;
        int totalQuestions = 10;
        int correctAnswers = 7;

        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(userStats);
        when(badgeRepository.findUnlockableBadges(anyString(), anyInt())).thenReturn(new ArrayList<>());
        when(userProgressRepository.findByUserIdAndTopic(userId, topic)).thenReturn(Optional.empty());
        when(userProgressRepository.save(any(UserProgress.class))).thenReturn(userProgress);

        // Act
        XpEventResponse response = gamificationService.recordQuizXp(user, true, topic, subject,
                score, totalQuestions, correctAnswers);

        // Assert
        assertThat(response).isNotNull();
        verify(userProgressRepository).save(userProgressCaptor.capture());
        UserProgress savedProgress = userProgressCaptor.getValue();
        assertThat(savedProgress.getTopic()).isEqualTo(topic);
        assertThat(savedProgress.getQuizCompleted()).isEqualTo(1);
        assertThat(savedProgress.getTotalQuestions()).isEqualTo(totalQuestions);
        assertThat(savedProgress.getCorrectAnswers()).isEqualTo(correctAnswers);
        assertThat(savedProgress.getAverageScore()).isEqualTo(70.0);
    }

    @Test
    void recordFlashcardXp_Success() {
        // Arrange
        int cardsStudied = 5;
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(userStats);
        when(badgeRepository.findUnlockableBadges(anyString(), anyInt())).thenReturn(new ArrayList<>());

        // Act
        XpEventResponse response = gamificationService.recordFlashcardXp(user, cardsStudied);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getXpEarned()).isEqualTo(10); // 5 * 2
        verify(userStatsRepository).save(userStatsCaptor.capture());
        UserStats savedStats = userStatsCaptor.getValue();
        assertThat(savedStats.getFlashcardsStudied()).isEqualTo(55);
        assertThat(savedStats.getTotalXp()).isEqualTo(510);
    }

    @Test
    void recordFocusSessionXp_Success() {
        // Arrange
        int durationMinutes = 25;
        int xpToAward = 15;
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(userStats);
        when(badgeRepository.findUnlockableBadges(anyString(), anyInt())).thenReturn(new ArrayList<>());

        // Act
        XpEventResponse response = gamificationService.recordFocusSessionXp(user, durationMinutes, xpToAward);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getXpEarned()).isEqualTo(xpToAward);
        verify(userStatsRepository).save(userStatsCaptor.capture());
        UserStats savedStats = userStatsCaptor.getValue();
        assertThat(savedStats.getFocusSessionsCompleted()).isEqualTo(4);
        assertThat(savedStats.getTotalStudyTimeMinutes()).isEqualTo(145);
        assertThat(savedStats.getTotalXp()).isEqualTo(515);
    }

    // ==================== USER PROGRESS TESTS ====================

    @Test
    void updateUserProgress_CreateNew_Success() {
        // Arrange
        String topic = "Calcolo";
        String subject = "Mathematics";
        when(userProgressRepository.findByUserIdAndTopic(userId, topic)).thenReturn(Optional.empty());
        when(userProgressRepository.save(any(UserProgress.class))).thenReturn(userProgress);

        // Act
        gamificationService.updateUserProgress(user, topic, subject, 1, 80.0, 10, 8);

        // Assert
        verify(userProgressRepository).save(userProgressCaptor.capture());
        UserProgress savedProgress = userProgressCaptor.getValue();
        assertThat(savedProgress.getUser()).isEqualTo(user);
        assertThat(savedProgress.getTopic()).isEqualTo(topic);
        assertThat(savedProgress.getSubject()).isEqualTo(subject);
        assertThat(savedProgress.getQuizCompleted()).isEqualTo(1);
        assertThat(savedProgress.getTotalQuestions()).isEqualTo(10);
        assertThat(savedProgress.getCorrectAnswers()).isEqualTo(8);
        assertThat(savedProgress.getAverageScore()).isEqualTo(80.0);
        assertThat(savedProgress.getMasteryLevel()).isEqualTo(DifficultyLevel.INTERMEDIO);
    }

    @Test
    void updateUserProgress_UpdateExisting_Success() {
        // Arrange
        String topic = "Algebra";
        when(userProgressRepository.findByUserIdAndTopic(userId, topic)).thenReturn(Optional.of(userProgress));
        when(userProgressRepository.save(any(UserProgress.class))).thenReturn(userProgress);

        // Act
        gamificationService.updateUserProgress(user, topic, "Mathematics", 2, 75.0, 20, 15);

        // Assert
        verify(userProgressRepository).save(userProgressCaptor.capture());
        UserProgress updatedProgress = userProgressCaptor.getValue();
        assertThat(updatedProgress.getQuizCompleted()).isEqualTo(5); // 3 + 2
        assertThat(updatedProgress.getTotalQuestions()).isEqualTo(50); // 30 + 20
        assertThat(updatedProgress.getCorrectAnswers()).isEqualTo(30); // 15 + 15
        assertThat(updatedProgress.getAverageScore()).isEqualTo(60.0); // 30/50*100
        assertThat(updatedProgress.getMasteryLevel()).isEqualTo(DifficultyLevel.PRINCIPIANTE); // 60% = PRINCIPIANTE
    }

    @Test
    void calculateMasteryLevel_Test() {
        // Test reflection per metodo privato
        assertThat(calculateMasteryLevelReflection(95.0)).isEqualTo(DifficultyLevel.AVANZATO);
        assertThat(calculateMasteryLevelReflection(85.0)).isEqualTo(DifficultyLevel.AVANZATO);
        assertThat(calculateMasteryLevelReflection(75.0)).isEqualTo(DifficultyLevel.INTERMEDIO);
        assertThat(calculateMasteryLevelReflection(65.0)).isEqualTo(DifficultyLevel.INTERMEDIO);
        assertThat(calculateMasteryLevelReflection(55.0)).isEqualTo(DifficultyLevel.PRINCIPIANTE);
        assertThat(calculateMasteryLevelReflection(0.0)).isEqualTo(DifficultyLevel.PRINCIPIANTE);
        assertThat(calculateMasteryLevelReflection(null)).isEqualTo(DifficultyLevel.PRINCIPIANTE);
    }

    private DifficultyLevel calculateMasteryLevelReflection(Double score) {
        try {
            var method = GamificationServiceImpl.class.getDeclaredMethod("calculateMasteryLevel", Double.class);
            method.setAccessible(true);
            return (DifficultyLevel) method.invoke(gamificationService, score);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== BADGE TESTS ====================

    @Test
    void getAllBadgesWithStatus_Success() {
        // Arrange
        List<Badge> allBadges = Arrays.asList(badge);
        when(badgeRepository.findByIsActiveTrueOrderByRequirementValueAsc()).thenReturn(allBadges);
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userBadgeRepository.findByUserIdOrderByUnlockedAtDesc(userId))
                .thenReturn(Arrays.asList(userBadge));

        // Act
        List<BadgeResponse> responses = gamificationService.getAllBadgesWithStatus(userId);

        // Assert
        assertThat(responses).hasSize(1);
        BadgeResponse response = responses.get(0);
        assertThat(response.getId()).isEqualTo(badgeId);
        assertThat(response.isUnlocked()).isTrue();
        assertThat(response.getUnlockedAt()).isNotNull();
        assertThat(response.getProgress()).isLessThanOrEqualTo(100.0);
    }

    @Test
    void getUnlockedBadges_Success() {
        // Arrange
        when(userBadgeRepository.findByUserIdOrderByUnlockedAtDesc(userId))
                .thenReturn(Arrays.asList(userBadge));

        // Act
        List<BadgeResponse> responses = gamificationService.getUnlockedBadges(userId);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(badgeId);
        assertThat(responses.get(0).isUnlocked()).isTrue();
    }

    @Test
    void getNewBadges_Success() {
        // Arrange
        when(userBadgeRepository.findByUserIdAndIsNewTrue(userId))
                .thenReturn(Arrays.asList(userBadge));

        // Act
        List<BadgeResponse> responses = gamificationService.getNewBadges(userId);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(badgeId);
    }

    @Test
    void markBadgesAsSeen_Success() {
        // Arrange
        doNothing().when(userBadgeRepository).markAllAsSeenForUser(userId);

        // Act
        gamificationService.markBadgesAsSeen(userId);

        // Assert
        verify(userBadgeRepository).markAllAsSeenForUser(userId);
    }

    @Test
    void checkAndUnlockBadges_NewBadgeUnlocked_Success() {
        // Arrange
        when(badgeRepository.findUnlockableBadges("QUIZZES_COMPLETED", 15))
                .thenReturn(Arrays.asList(badge));
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId))
                .thenReturn(false);
        when(userBadgeRepository.save(any(UserBadge.class))).thenReturn(userBadge);
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(userStats);

        // Act
        List<Badge> newBadges = gamificationService.checkAndUnlockBadges(user, userStats);

        // Assert
        assertThat(newBadges).hasSize(1);
        assertThat(newBadges.get(0).getId()).isEqualTo(badgeId);

        verify(userBadgeRepository).save(userBadgeCaptor.capture());
        UserBadge savedBadge = userBadgeCaptor.getValue();
        assertThat(savedBadge.getUser()).isEqualTo(user);
        assertThat(savedBadge.getBadge()).isEqualTo(badge);
        assertThat(savedBadge.getProgressAtUnlock()).isEqualTo(15);

        verify(userStatsRepository).save(userStatsCaptor.capture());
        UserStats savedStats = userStatsCaptor.getValue();
        assertThat(savedStats.getTotalXp()).isEqualTo(550); // 500 + 50
    }

    @Test
    void checkAndUnlockBadges_AlreadyUnlocked_Success() {
        // Arrange
        when(badgeRepository.findUnlockableBadges("QUIZZES_COMPLETED", 15))
                .thenReturn(Arrays.asList(badge));
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId))
                .thenReturn(true);

        // Act
        List<Badge> newBadges = gamificationService.checkAndUnlockBadges(user, userStats);

        // Assert
        assertThat(newBadges).isEmpty();
        verify(userBadgeRepository, never()).save(any());
        verify(userStatsRepository, never()).save(any());
    }

    @Test
    void calculateBadgeProgress_Success() {
        // Test metodo privato calculateBadgeProgress via reflection
        Double progress = calculateBadgeProgressReflection(badge, userStats);

        assertThat(progress).isEqualTo(75.0); // 15/20 * 100
    }

    private Double calculateBadgeProgressReflection(Badge badge, UserStats stats) {
        try {
            var method = GamificationServiceImpl.class.getDeclaredMethod("calculateBadgeProgress", Badge.class, UserStats.class);
            method.setAccessible(true);
            return (Double) method.invoke(gamificationService, badge, stats);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== RECOMMENDATION TESTS ====================

    @Test
    void getActiveRecommendations_Success() {
        // Arrange
        when(recommendationRepository.findActiveByUserId(eq(userId), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(recommendation));

        // Act
        List<RecommendationResponse> responses = gamificationService.getActiveRecommendations(userId);

        // Assert
        assertThat(responses).hasSize(1);
        RecommendationResponse response = responses.get(0);
        assertThat(response.getId()).isEqualTo(recommendationId);
        assertThat(response.getTitle()).isEqualTo(recommendation.getTitle());
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void generateRecommendations_WithStreakReminder_Success() {
        // Arrange
        userStats.setCurrentStreak(5);
        userStats.setLastActivityDate(LocalDate.now().minusDays(1));

        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userProgressRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
        when(recommendationRepository.existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
                eq(userId), eq(RecommendationType.STREAK_REMINDER), isNull()))
                .thenReturn(false);
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        List<Recommendation> recommendations = gamificationService.generateRecommendations(user);

        // Assert
        assertThat(recommendations).isNotEmpty();
        boolean hasStreakRec = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.STREAK_REMINDER);
        assertThat(hasStreakRec).isTrue();
    }

    @Test
    void generateRecommendations_WithWeakTopics_Success() {
        // Arrange
        userProgress.setAverageScore(55.0);
        userProgress.setQuizCompleted(5);

        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userProgressRepository.findByUserId(userId)).thenReturn(Arrays.asList(userProgress));
        when(recommendationRepository.existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
                eq(userId), eq(RecommendationType.WEAKNESS_FOCUS), eq("Algebra")))
                .thenReturn(false);
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        List<Recommendation> recommendations = gamificationService.generateRecommendations(user);

        // Assert
        assertThat(recommendations).isNotEmpty();
        boolean hasWeakTopicRec = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.WEAKNESS_FOCUS
                        && "Algebra".equals(r.getTopic()));
        assertThat(hasWeakTopicRec).isTrue();
    }

    @Test
    void generateRecommendations_WithReviewTopics_Success() {
        // Arrange
        userProgress.setLastActivityAt(LocalDateTime.now().minusDays(10));

        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userProgressRepository.findByUserId(userId)).thenReturn(Arrays.asList(userProgress));
        when(recommendationRepository.existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
                eq(userId), eq(RecommendationType.REVIEW_TOPIC), eq("Algebra")))
                .thenReturn(false);
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        List<Recommendation> recommendations = gamificationService.generateRecommendations(user);

        // Assert
        assertThat(recommendations).isNotEmpty();
        boolean hasReviewRec = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.REVIEW_TOPIC
                        && "Algebra".equals(r.getTopic()));
        assertThat(hasReviewRec).isTrue();
    }

    @Test
    void generateRecommendations_WithDailyGoal_Success() {
        // Arrange
        userStats.setWeeklyXp(30);

        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userProgressRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
        when(recommendationRepository.existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
                eq(userId), eq(RecommendationType.DAILY_GOAL), isNull()))
                .thenReturn(false);
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        List<Recommendation> recommendations = gamificationService.generateRecommendations(user);

        // Assert
        assertThat(recommendations).isNotEmpty();
        boolean hasDailyGoalRec = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.DAILY_GOAL);
        assertThat(hasDailyGoalRec).isTrue();
    }

    @Test
    void generateRecommendations_WithSuggestFeatures_Success() {
        // Arrange
        userStats.setQuizzesCompleted(0);
        userStats.setFlashcardsStudied(0);

        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(userStats));
        when(userProgressRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
        when(recommendationRepository.existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
                eq(userId), eq(RecommendationType.NEW_TOPIC), isNull()))
                .thenReturn(false);
        when(recommendationRepository.existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
                eq(userId), eq(RecommendationType.STUDY_FLASHCARDS), isNull()))
                .thenReturn(false);
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        List<Recommendation> recommendations = gamificationService.generateRecommendations(user);

        // Assert
        assertThat(recommendations).isNotEmpty();
        boolean hasQuizRec = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.NEW_TOPIC);
        boolean hasFlashcardRec = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.STUDY_FLASHCARDS);
        assertThat(hasQuizRec).isTrue();
        assertThat(hasFlashcardRec).isTrue();
    }

    @Test
    void createRecommendation_AlreadyExists_ReturnsNull() {
        // Arrange
        when(recommendationRepository.existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
                userId, RecommendationType.WEAKNESS_FOCUS, "Algebra"))
                .thenReturn(true);

        // Act & Assert - test metodo privato via reflection
        Recommendation result = createRecommendationReflection(user, RecommendationType.WEAKNESS_FOCUS,
                "Test", "Desc", "Algebra", "Reason", Priority.HIGH);

        assertThat(result).isNull();
    }

    private Recommendation createRecommendationReflection(User user, RecommendationType type,
                                                          String title, String description,
                                                          String topic, String reason, Priority priority) {
        try {
            var method = GamificationServiceImpl.class.getDeclaredMethod("createRecommendation",
                    User.class, RecommendationType.class, String.class, String.class,
                    String.class, String.class, Priority.class);
            method.setAccessible(true);
            return (Recommendation) method.invoke(gamificationService, user, type, title, description,
                    topic, reason, priority);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void dismissRecommendation_Success() {
        // Arrange
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        // Act
        gamificationService.dismissRecommendation(recommendationId, userId);

        // Assert
        verify(recommendationRepository).save(recommendationCaptor.capture());
        Recommendation savedRec = recommendationCaptor.getValue();
        assertThat(savedRec.getIsDismissed()).isTrue();
    }

    @Test
    void dismissRecommendation_Unauthorized_ThrowsException() {
        // Arrange
        UUID otherUserId = UUID.randomUUID();
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        // Act & Assert
        assertThatThrownBy(() -> gamificationService.dismissRecommendation(recommendationId, otherUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Non autorizzato");
    }

    @Test
    void completeRecommendation_Success() {
        // Arrange
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        // Act
        gamificationService.completeRecommendation(recommendationId, userId);

        // Assert
        verify(recommendationRepository).save(recommendationCaptor.capture());
        Recommendation savedRec = recommendationCaptor.getValue();
        assertThat(savedRec.getIsCompleted()).isTrue();
    }

    // ==================== LEADERBOARD TESTS ====================

    @Test
    void getXpLeaderboard_Success() {
        // Arrange
        int limit = 10;
        List<UserStats> topUsers = Arrays.asList(userStats);
        when(userStatsRepository.findTopByTotalXp(limit)).thenReturn(topUsers);

        // Act
        List<LeaderboardEntry> entries = gamificationService.getXpLeaderboard(limit);

        // Assert
        assertThat(entries).hasSize(1);
        LeaderboardEntry entry = entries.get(0);
    }

    @Test
    void getWeeklyLeaderboard_Success() {
        // Arrange
        int limit = 10;
        List<UserStats> topUsers = Arrays.asList(userStats);
        when(userStatsRepository.findTopByWeeklyXp(limit)).thenReturn(topUsers);

        // Act
        List<LeaderboardEntry> entries = gamificationService.getWeeklyLeaderboard(limit);

        // Assert
        assertThat(entries).hasSize(1);
        LeaderboardEntry entry = entries.get(0);
    }

    @Test
    void getStreakLeaderboard_Success() {
        // Arrange
        int limit = 10;
        List<UserStats> topUsers = Arrays.asList(userStats);
        when(userStatsRepository.findTopByStreak(limit)).thenReturn(topUsers);

        // Act
        List<LeaderboardEntry> entries = gamificationService.getStreakLeaderboard(limit);

        // Assert
        assertThat(entries).hasSize(1);
        LeaderboardEntry entry = entries.get(0);
    }

    @Test
    void getUserRank_Found_Success() {
        // Arrange
        UserStats stats1 = new UserStats();
        stats1.setUser(createUserWithId(UUID.randomUUID()));
        UserStats stats2 = new UserStats();
        stats2.setUser(createUserWithId(userId));
        UserStats stats3 = new UserStats();
        stats3.setUser(createUserWithId(UUID.randomUUID()));

        List<UserStats> topUsers = Arrays.asList(stats1, stats2, stats3);
        when(userStatsRepository.findTopByTotalXp(1000)).thenReturn(topUsers);

        // Act
        int rank = gamificationService.getUserRank(userId, "XP");

        // Assert
        assertThat(rank).isEqualTo(2);
    }

    @Test
    void getUserRank_NotFound_ReturnsMinusOne() {
        // Arrange
        List<UserStats> topUsers = Arrays.asList(
                createUserStatsWithUserId(UUID.randomUUID()),
                createUserStatsWithUserId(UUID.randomUUID())
        );
        when(userStatsRepository.findTopByTotalXp(1000)).thenReturn(topUsers);

        // Act
        int rank = gamificationService.getUserRank(userId, "XP");

        // Assert
        assertThat(rank).isEqualTo(-1);
    }

    private User createUserWithId(UUID id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private UserStats createUserStatsWithUserId(UUID id) {
        UserStats stats = new UserStats();
        stats.setUser(createUserWithId(id));
        return stats;
    }
}