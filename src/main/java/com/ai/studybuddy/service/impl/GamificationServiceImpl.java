package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.gamification.GamificationDTO.*;
import com.ai.studybuddy.dto.recommendation.RecommendationResponse;
import com.ai.studybuddy.model.gamification.*;
import com.ai.studybuddy.model.recommendation.Recommendation;
import com.ai.studybuddy.model.recommendation.Recommendation.Priority;
import com.ai.studybuddy.model.recommendation.Recommendation.RecommendationType;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.model.user.UserProgress;
import com.ai.studybuddy.repository.*;
import com.ai.studybuddy.service.inter.GamificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementazione GamificationService
 *
 * INTEGRAZIONE con UserProgress:
 * - Quando si registrano attività (quiz, spiegazioni, etc.)
 *   questo service aggiorna ENTRAMBE le tabelle:
 *   1. UserProgress → per tracciare progressi per argomento
 *   2. UserStats → per XP globali, badge, streak
 */
@Service
public class GamificationServiceImpl implements GamificationService {

    private static final Logger logger = LoggerFactory.getLogger(GamificationServiceImpl.class);

    // ==================== COSTANTI XP (dalla documentazione) ====================
    private static final int XP_EXPLANATION = 10;      // +10 XP per spiegazione
    private static final int XP_QUIZ_COMPLETED = 20;   // +20 XP per quiz completato
    private static final int XP_QUIZ_PASSED_BONUS = 10;// +10 XP bonus se superato
    private static final int XP_FLASHCARD_PER_CARD = 2;// +2 XP per flashcard
    private static final int XP_FOCUS_SESSION = 15;    // +15 XP per sessione focus

    private final UserStatsRepository userStatsRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final UserProgressRepository userProgressRepository;

    public GamificationServiceImpl(
            UserStatsRepository userStatsRepository,
            BadgeRepository badgeRepository,
            UserBadgeRepository userBadgeRepository,
            RecommendationRepository recommendationRepository,
            UserRepository userRepository,
            UserProgressRepository userProgressRepository) {
        this.userStatsRepository = userStatsRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
        this.userProgressRepository = userProgressRepository;
    }

    // ==================== XP & STATISTICHE ====================

    public UserStats getOrCreateUserStats(UUID userId) {
        return userStatsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Utente non trovato"));
                    UserStats stats = new UserStats();
                    stats.setUser(user);
                    return userStatsRepository.save(stats);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStatsResponse(UUID userId) {
        UserStats stats = getOrCreateUserStats(userId);
        long badgeCount = userBadgeRepository.countByUserId(userId);

        // Arricchisci con dati da UserProgress
        enrichStatsFromProgress(stats, userId);

        return UserStatsResponse.fromUserStats(stats, badgeCount);
    }

    /**
     * Arricchisce UserStats con dati aggregati da UserProgress
     * Nota: i minuti di studio sono già tracciati in UserStats.totalStudyTimeMinutes
     */
    private void enrichStatsFromProgress(UserStats stats, UUID userId) {
        // I dati aggregati da UserProgress (come quiz per topic, score medio, etc.)
        // sono già disponibili tramite UserProgressRepository se necessario.
        // UserStats traccia già totalStudyTimeMinutes direttamente.
    }

    @Override
    @Transactional
    public XpEventResponse recordExplanationXp(User user) {
        return recordExplanationXp(user, null, null);
    }

    /**
     * Registra XP per spiegazione con topic opzionale
     * Aggiorna sia UserStats che UserProgress
     */
    @Transactional
    public XpEventResponse recordExplanationXp(User user, String topic, String subject) {
        UserStats stats = getOrCreateUserStats(user.getId());

        // Aggiorna UserStats (globale)
        stats.incrementExplanations();
        stats.updateStreak();
        boolean leveledUp = stats.addXp(XP_EXPLANATION);
        userStatsRepository.save(stats);

        // Aggiorna UserProgress (per topic) se specificato
        if (topic != null && !topic.isEmpty()) {
            updateUserProgress(user, topic, subject, 0, 0, 0, 0);
        }

        // Verifica badge
        List<Badge> newBadges = checkAndUnlockBadges(user, stats);

        logger.info("Utente {} ha guadagnato {} XP per spiegazione. Totale: {}",
                user.getEmail(), XP_EXPLANATION, stats.getTotalXp());

        return new XpEventResponse("EXPLANATION", XP_EXPLANATION, stats, leveledUp, newBadges);
    }

    @Override
    @Transactional
    public XpEventResponse recordQuizXp(User user, boolean passed) {
        return recordQuizXp(user, passed, null, null, 0, 0, 0);
    }

    /**
     * Registra XP per quiz con dettagli per UserProgress
     */
    @Transactional
    public XpEventResponse recordQuizXp(User user, boolean passed, String topic, String subject,
                                        double score, int totalQuestions, int correctAnswers) {
        UserStats stats = getOrCreateUserStats(user.getId());

        int xpEarned = XP_QUIZ_COMPLETED;
        if (passed) {
            xpEarned += XP_QUIZ_PASSED_BONUS;
        }

        // Aggiorna UserStats (globale)
        stats.incrementQuizzesCompleted(passed);
        stats.updateStreak();
        boolean leveledUp = stats.addXp(xpEarned);
        userStatsRepository.save(stats);

        // Aggiorna UserProgress (per topic) se specificato
        if (topic != null && !topic.isEmpty()) {
            updateUserProgress(user, topic, subject, 1, score, totalQuestions, correctAnswers);
        }

        // Verifica badge
        List<Badge> newBadges = checkAndUnlockBadges(user, stats);

        logger.info("Utente {} ha guadagnato {} XP per quiz (passed: {}). Totale: {}",
                user.getEmail(), xpEarned, passed, stats.getTotalXp());

        return new XpEventResponse("QUIZ", xpEarned, stats, leveledUp, newBadges);
    }

    @Override
    @Transactional
    public XpEventResponse recordFlashcardXp(User user, int cardsStudied) {
        UserStats stats = getOrCreateUserStats(user.getId());

        int xpEarned = cardsStudied * XP_FLASHCARD_PER_CARD;

        stats.incrementFlashcardsStudied(cardsStudied);
        stats.updateStreak();
        boolean leveledUp = stats.addXp(xpEarned);
        userStatsRepository.save(stats);

        List<Badge> newBadges = checkAndUnlockBadges(user, stats);

        logger.info("Utente {} ha guadagnato {} XP per {} flashcards. Totale: {}",
                user.getEmail(), xpEarned, cardsStudied, stats.getTotalXp());

        return new XpEventResponse("FLASHCARD", xpEarned, stats, leveledUp, newBadges);
    }

    @Override
    @Transactional
    public XpEventResponse recordFocusSessionXp(User user, int durationMinutes, int xpToAward) {
        UserStats stats = getOrCreateUserStats(user.getId());

        stats.incrementFocusSessions(durationMinutes);
        stats.updateStreak();
        boolean leveledUp = stats.addXp(xpToAward);
        userStatsRepository.save(stats);

        List<Badge> newBadges = checkAndUnlockBadges(user, stats);

        logger.info("Utente {} ha guadagnato {} XP per sessione focus ({} min). Totale: {}",
                user.getEmail(), xpToAward, durationMinutes, stats.getTotalXp());

        return new XpEventResponse("FOCUS_SESSION", xpToAward, stats, leveledUp, newBadges);
    }

    // ==================== INTEGRAZIONE USER PROGRESS ====================

    /**
     * Aggiorna o crea UserProgress per un topic specifico
     */
    @Transactional
    public void updateUserProgress(User user, String topic, String subject,
                                   int quizCompleted, double score,
                                   int totalQuestions, int correctAnswers) {
        try {
            Optional<UserProgress> existingProgress = userProgressRepository
                    .findByUserIdAndTopic(user.getId(), topic);

            UserProgress progress;
            if (existingProgress.isPresent()) {
                progress = existingProgress.get();

                // Aggiorna statistiche esistenti
                progress.setQuizCompleted(
                        (progress.getQuizCompleted() != null ? progress.getQuizCompleted() : 0) + quizCompleted);
                progress.setTotalQuestions(
                        (progress.getTotalQuestions() != null ? progress.getTotalQuestions() : 0) + totalQuestions);
                progress.setCorrectAnswers(
                        (progress.getCorrectAnswers() != null ? progress.getCorrectAnswers() : 0) + correctAnswers);

                // Ricalcola media
                if (progress.getTotalQuestions() > 0) {
                    double newAverage = (progress.getCorrectAnswers() * 100.0) / progress.getTotalQuestions();
                    progress.setAverageScore(newAverage);
                }

            } else {
                // Crea nuovo progress
                progress = new UserProgress();
                progress.setUser(user);
                progress.setTopic(topic);
                progress.setSubject(subject);
                progress.setQuizCompleted(quizCompleted);
                progress.setTotalQuestions(totalQuestions);
                progress.setCorrectAnswers(correctAnswers);

                if (totalQuestions > 0) {
                    progress.setAverageScore((correctAnswers * 100.0) / totalQuestions);
                }
            }

            progress.setLastActivityAt(LocalDateTime.now());

            // Calcola mastery level basato su score medio
            progress.setMasteryLevel(calculateMasteryLevel(progress.getAverageScore()));

            userProgressRepository.save(progress);
        } catch (Exception e) {
            logger.warn("Errore aggiornamento UserProgress: {}", e.getMessage());
        }
    }

    /**
     * Calcola il livello di padronanza basato sul punteggio medio
     */
    private com.ai.studybuddy.util.enums.DifficultyLevel calculateMasteryLevel(Double averageScore) {
        if (averageScore == null) return com.ai.studybuddy.util.enums.DifficultyLevel.PRINCIPIANTE;

        if (averageScore >= 90) return com.ai.studybuddy.util.enums.DifficultyLevel.AVANZATO;
        if (averageScore >= 70) return com.ai.studybuddy.util.enums.DifficultyLevel.INTERMEDIO;
        return com.ai.studybuddy.util.enums.DifficultyLevel.PRINCIPIANTE;
    }

    // ==================== BADGE ====================

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> getAllBadgesWithStatus(UUID userId) {
        List<Badge> allBadges = badgeRepository.findByIsActiveTrueOrderByRequirementValueAsc();
        UserStats stats = getOrCreateUserStats(userId);

        Map<UUID, UserBadge> unlockedMap = userBadgeRepository.findByUserIdOrderByUnlockedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getId(), ub -> ub));

        return allBadges.stream()
                .map(badge -> {
                    UserBadge userBadge = unlockedMap.get(badge.getId());
                    boolean unlocked = userBadge != null;
                    LocalDateTime unlockedAt = unlocked ? userBadge.getUnlockedAt() : null;
                    Double progress = calculateBadgeProgress(badge, stats);
                    return BadgeResponse.fromBadge(badge, unlocked, unlockedAt, progress);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> getUnlockedBadges(UUID userId) {
        return userBadgeRepository.findByUserIdOrderByUnlockedAtDesc(userId)
                .stream()
                .map(BadgeResponse::fromUserBadge)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> getNewBadges(UUID userId) {
        return userBadgeRepository.findByUserIdAndIsNewTrue(userId)
                .stream()
                .map(BadgeResponse::fromUserBadge)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markBadgesAsSeen(UUID userId) {
        userBadgeRepository.markAllAsSeenForUser(userId);
    }

    @Override
    @Transactional
    public List<Badge> checkAndUnlockBadges(User user, UserStats stats) {
        List<Badge> newlyUnlocked = new ArrayList<>();

        // Verifica ogni tipo di requisito
        checkAndUnlockByType(user, stats, "EXPLANATIONS_COUNT",
                stats.getExplanationsRequested(), newlyUnlocked);
        checkAndUnlockByType(user, stats, "QUIZZES_COMPLETED",
                stats.getQuizzesCompleted(), newlyUnlocked);
        checkAndUnlockByType(user, stats, "QUIZZES_PASSED",
                stats.getQuizzesPassed(), newlyUnlocked);
        checkAndUnlockByType(user, stats, "FLASHCARDS_STUDIED",
                stats.getFlashcardsStudied(), newlyUnlocked);
        checkAndUnlockByType(user, stats, "STREAK_DAYS",
                stats.getCurrentStreak(), newlyUnlocked);
        checkAndUnlockByType(user, stats, "TOTAL_XP",
                stats.getTotalXp(), newlyUnlocked);
        checkAndUnlockByType(user, stats, "LEVEL",
                stats.getLevel(), newlyUnlocked);
        checkAndUnlockByType(user, stats, "FOCUS_SESSIONS",
                stats.getFocusSessionsCompleted(), newlyUnlocked);

        return newlyUnlocked;
    }

    private void checkAndUnlockByType(User user, UserStats stats, String type,
                                      Integer currentValue, List<Badge> newlyUnlocked) {
        if (currentValue == null) return;

        List<Badge> eligibleBadges = badgeRepository.findUnlockableBadges(type, currentValue);

        for (Badge badge : eligibleBadges) {
            if (!userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
                // Sblocca il badge
                UserBadge userBadge = new UserBadge();
                userBadge.setUser(user);
                userBadge.setBadge(badge);
                userBadge.setProgressAtUnlock(currentValue);
                userBadgeRepository.save(userBadge);

                // Aggiungi XP bonus del badge
                if (badge.getXpReward() != null && badge.getXpReward() > 0) {
                    stats.addXp(badge.getXpReward());
                }

                newlyUnlocked.add(badge);
                logger.info("Utente {} ha sbloccato il badge: {}", user.getEmail(), badge.getName());
            }
        }
    }

    private Double calculateBadgeProgress(Badge badge, UserStats stats) {
        if (badge.getRequirementType() == null || badge.getRequirementValue() == null) {
            return 0.0;
        }

        Integer currentValue = switch (badge.getRequirementType()) {
            case "EXPLANATIONS_COUNT" -> stats.getExplanationsRequested();
            case "QUIZZES_COMPLETED" -> stats.getQuizzesCompleted();
            case "QUIZZES_PASSED" -> stats.getQuizzesPassed();
            case "FLASHCARDS_STUDIED" -> stats.getFlashcardsStudied();
            case "STREAK_DAYS" -> stats.getCurrentStreak();
            case "TOTAL_XP" -> stats.getTotalXp();
            case "LEVEL" -> stats.getLevel();
            case "FOCUS_SESSIONS" -> stats.getFocusSessionsCompleted();
            default -> 0;
        };

        if (currentValue == null) return 0.0;

        double progress = (double) currentValue / badge.getRequirementValue() * 100;
        return Math.min(progress, 100.0);
    }

    // ==================== LEADERBOARD ====================

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getXpLeaderboard(int limit) {
        return userStatsRepository.findTopByTotalXp(limit)
                .stream()
                .map(stats -> new LeaderboardEntry(0, stats, "XP"))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getWeeklyLeaderboard(int limit) {
        return userStatsRepository.findTopByWeeklyXp(limit)
                .stream()
                .map(stats -> new LeaderboardEntry(0, stats, "WEEKLY_XP"))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getStreakLeaderboard(int limit) {
        return userStatsRepository.findTopByStreak(limit)
                .stream()
                .map(stats -> new LeaderboardEntry(0, stats, "STREAK"))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public int getUserRank(UUID userId, String type) {
        List<UserStats> allStats = switch (type) {
            case "XP" -> userStatsRepository.findTopByTotalXp(1000);
            case "WEEKLY_XP" -> userStatsRepository.findTopByWeeklyXp(1000);
            case "STREAK" -> userStatsRepository.findTopByStreak(1000);
            default -> userStatsRepository.findTopByLevel(1000);
        };

        for (int i = 0; i < allStats.size(); i++) {
            if (allStats.get(i).getUser().getId().equals(userId)) {
                return i + 1;
            }
        }
        return -1;
    }
}