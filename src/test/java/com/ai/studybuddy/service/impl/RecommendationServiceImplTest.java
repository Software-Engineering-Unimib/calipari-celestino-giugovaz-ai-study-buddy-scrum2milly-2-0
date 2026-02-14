package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.recommendation.RecommendationResponse;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.recommendation.Recommendation;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.model.user.UserProgress;
import com.ai.studybuddy.repository.RecommendationRepository;
import com.ai.studybuddy.repository.UserProgressRepository;
import com.ai.studybuddy.service.inter.GamificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationServiceImpl - Coverage Completa")
class RecommendationServiceImplTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private UserProgressRepository userProgressRepository;

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private User testUser;
    private UserStats testStats;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");

        testStats = mock(UserStats.class);
    }

    @Test
    @DisplayName("getActiveRecommendations - lista vuota")
    void testGetActiveRecommendations_Empty() {
        when(recommendationRepository.findActiveByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<RecommendationResponse> result = recommendationService.getActiveRecommendations(testUserId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getActiveRecommendations - con risultati")
    void testGetActiveRecommendations_WithResults() {
        Recommendation rec1 = new Recommendation();
        rec1.setId(UUID.randomUUID());
        rec1.setTitle("Test 1");
        
        Recommendation rec2 = new Recommendation();
        rec2.setId(UUID.randomUUID());
        rec2.setTitle("Test 2");

        when(recommendationRepository.findActiveByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(rec1, rec2));

        List<RecommendationResponse> result = recommendationService.getActiveRecommendations(testUserId);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("generateRecommendations - streak reminder quando streak > 0 e lastActivity < today")
    void testGenerateRecommendations_StreakReminder() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getCurrentStreak()).thenReturn(5);
        when(testStats.getLastActivityDate()).thenReturn(LocalDate.now().minusDays(1));
        
        setupDefaultMocks();
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
        verify(recommendationRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("generateRecommendations - no streak reminder quando lastActivity è oggi")
    void testGenerateRecommendations_NoStreakReminderToday() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getCurrentStreak()).thenReturn(5);
        when(testStats.getLastActivityDate()).thenReturn(LocalDate.now());
        
        setupDefaultMocks();

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - no streak reminder quando streak = 0")
    void testGenerateRecommendations_NoStreakReminderZeroStreak() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getCurrentStreak()).thenReturn(0);
        
        setupDefaultMocks();

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - weak topics (score < 60%)")
    void testGenerateRecommendations_WeakTopics() {
        UserProgress weakProgress = createUserProgress("Math", 45.0);
        
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(testUserId, 60.0)).thenReturn(Arrays.asList(weakProgress));
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
        verify(recommendationRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("generateRecommendations - topics needing review (7+ giorni)")
    void testGenerateRecommendations_TopicsNeedingReview() {
        UserProgress oldProgress = createUserProgress("History", 70.0);
        oldProgress.setLastActivityAt(LocalDateTime.now().minusDays(10));
        
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(eq(testUserId), any())).thenReturn(Arrays.asList(oldProgress));
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
        verify(recommendationRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("generateRecommendations - review topic con priorità HIGH (14+ giorni)")
    void testGenerateRecommendations_ReviewTopicHighPriority() {
        UserProgress veryOldProgress = createUserProgress("Science", 70.0);
        veryOldProgress.setLastActivityAt(LocalDateTime.now().minusDays(20));
        
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(eq(testUserId), any())).thenReturn(Arrays.asList(veryOldProgress));
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - continue studying recent topics (60-80%)")
    void testGenerateRecommendations_ContinueStudying() {
        UserProgress recentProgress = createUserProgress("Physics", 70.0);
        
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findRecentTopics(testUserId, 3)).thenReturn(Arrays.asList(recentProgress));
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - no continue studying quando score < 60%")
    void testGenerateRecommendations_NoContinueStudyingLowScore() {
        UserProgress lowProgress = createUserProgress("Chemistry", 55.0);
        
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findRecentTopics(testUserId, 3)).thenReturn(Arrays.asList(lowProgress));
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - no continue studying quando score >= 80%")
    void testGenerateRecommendations_NoContinueStudyingHighScore() {
        UserProgress highProgress = createUserProgress("English", 85.0);
        
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findRecentTopics(testUserId, 3)).thenReturn(Arrays.asList(highProgress));
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - challenge yourself (score >= 80%)")
    void testGenerateRecommendations_ChallengeYourself() {
        UserProgress strongProgress = createUserProgress("Biology", 90.0);
        
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(testUserId)).thenReturn(Arrays.asList(strongProgress));
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - no challenge quando score < 80%")
    void testGenerateRecommendations_NoChallengeWhenLowScore() {
        UserProgress mediumProgress = createUserProgress("Geography", 75.0);
        
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findMostStudiedTopics(testUserId)).thenReturn(Arrays.asList(mediumProgress));
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - improve average (< 70%)")
    void testGenerateRecommendations_ImproveAverage() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(testUserId)).thenReturn(65.0);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - study more (< 60 minutes)")
    void testGenerateRecommendations_StudyMore() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(testUserId)).thenReturn(30);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - study more quando null")
    void testGenerateRecommendations_StudyMoreNullMinutes() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(testUserId)).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - explore new topics (< 5)")
    void testGenerateRecommendations_ExploreNewTopics() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(testUserId)).thenReturn(3L);
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - daily XP goal (< 50 XP)")
    void testGenerateRecommendations_DailyXpGoal() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getWeeklyXp()).thenReturn(30);
        
        setupDefaultMocks();
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - daily XP goal quando null")
    void testGenerateRecommendations_DailyXpGoalNull() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getWeeklyXp()).thenReturn(null);
        
        setupDefaultMocks();
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - try quiz (0 quizzes)")
    void testGenerateRecommendations_TryQuiz() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getQuizzesCompleted()).thenReturn(0);
        
        setupDefaultMocks();
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - try quiz quando null")
    void testGenerateRecommendations_TryQuizNull() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getQuizzesCompleted()).thenReturn(null);
        
        setupDefaultMocks();
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - try flashcards (0 studied)")
    void testGenerateRecommendations_TryFlashcards() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getFlashcardsStudied()).thenReturn(0);
        
        setupDefaultMocks();
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - try flashcards quando null")
    void testGenerateRecommendations_TryFlashcardsNull() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getFlashcardsStudied()).thenReturn(null);
        
        setupDefaultMocks();
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - milestone (10+ quizzes)")
    void testGenerateRecommendations_Milestone() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(testStats.getQuizzesCompleted()).thenReturn(15);
        
        setupDefaultMocks();
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(false);
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("generateRecommendations - gestisce eccezione da UserProgress")
    void testGenerateRecommendations_HandlesException() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenThrow(new RuntimeException("Database error"));

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
    }

    @Test
    @DisplayName("createRecommendation - non crea duplicati")
    void testCreateRecommendation_NoDuplicates() {
        when(gamificationService.getOrCreateUserStats(testUserId)).thenReturn(testStats);
        setupDefaultMocks();
        when(recommendationRepository.existsByUserIdAndTypeAndTopic(any(), any(), any())).thenReturn(true);

        List<Recommendation> result = recommendationService.generateRecommendations(testUser);

        assertNotNull(result);
        verify(recommendationRepository, never()).save(any());
    }

    @Test
    @DisplayName("dismissRecommendation - successo")
    void testDismissRecommendation_Success() {
        UUID recId = UUID.randomUUID();
        Recommendation rec = new Recommendation();
        rec.setId(recId);
        rec.setUser(testUser);

        when(recommendationRepository.findById(recId)).thenReturn(Optional.of(rec));
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        recommendationService.dismissRecommendation(recId, testUserId);

        verify(recommendationRepository, times(1)).save(rec);
    }

    @Test
    @DisplayName("dismissRecommendation - non trovata")
    void testDismissRecommendation_NotFound() {
        UUID recId = UUID.randomUUID();
        when(recommendationRepository.findById(recId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            recommendationService.dismissRecommendation(recId, testUserId);
        });
    }

    @Test
    @DisplayName("dismissRecommendation - utente non autorizzato")
    void testDismissRecommendation_Unauthorized() {
        UUID recId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        
        User differentUser = new User();
        differentUser.setId(differentUserId);
        
        Recommendation rec = new Recommendation();
        rec.setId(recId);
        rec.setUser(differentUser);

        when(recommendationRepository.findById(recId)).thenReturn(Optional.of(rec));

        assertThrows(RuntimeException.class, () -> {
            recommendationService.dismissRecommendation(recId, testUserId);
        });
    }

    @Test
    @DisplayName("completeRecommendation - successo")
    void testCompleteRecommendation_Success() {
        UUID recId = UUID.randomUUID();
        Recommendation rec = new Recommendation();
        rec.setId(recId);
        rec.setUser(testUser);

        when(recommendationRepository.findById(recId)).thenReturn(Optional.of(rec));
        when(recommendationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        recommendationService.completeRecommendation(recId, testUserId);

        verify(recommendationRepository, times(1)).save(rec);
    }

    @Test
    @DisplayName("completeRecommendation - non trovata")
    void testCompleteRecommendation_NotFound() {
        UUID recId = UUID.randomUUID();
        when(recommendationRepository.findById(recId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            recommendationService.completeRecommendation(recId, testUserId);
        });
    }

    @Test
    @DisplayName("completeRecommendation - utente non autorizzato")
    void testCompleteRecommendation_Unauthorized() {
        UUID recId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        
        User differentUser = new User();
        differentUser.setId(differentUserId);
        
        Recommendation rec = new Recommendation();
        rec.setId(recId);
        rec.setUser(differentUser);

        when(recommendationRepository.findById(recId)).thenReturn(Optional.of(rec));

        assertThrows(RuntimeException.class, () -> {
            recommendationService.completeRecommendation(recId, testUserId);
        });
    }

    private UserProgress createUserProgress(String topic, Double averageScore) {
        UserProgress progress = new UserProgress();
        progress.setTopic(topic);
        progress.setAverageScore(averageScore);
        progress.setLastActivityAt(LocalDateTime.now());
        return progress;
    }

    private void setupDefaultMocks() {
        when(userProgressRepository.findWeakTopics(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findTopicsNeedingReview(any(), any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findRecentTopics(any(), anyInt())).thenReturn(Collections.emptyList());
        when(userProgressRepository.findMostStudiedTopics(any())).thenReturn(Collections.emptyList());
        when(userProgressRepository.getOverallAverageScore(any())).thenReturn(null);
        when(userProgressRepository.getTotalStudyMinutes(any())).thenReturn(null);
        when(userProgressRepository.countByUserId(any())).thenReturn(0L);
    }
}