package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.gamification.GamificationDTO.*;
import com.ai.studybuddy.model.gamification.*;
import com.ai.studybuddy.model.gamification.Recommendation.Priority;
import com.ai.studybuddy.model.gamification.Recommendation.RecommendationType;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.*;
import com.ai.studybuddy.service.inter.GamificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GamificationServiceImpl implements GamificationService {

    private static final Logger logger = LoggerFactory.getLogger(GamificationServiceImpl.class);

    // Costanti XP dalla documentazione
    private static final int XP_EXPLANATION = 10;
    private static final int XP_QUIZ_COMPLETED = 20;
    private static final int XP_QUIZ_PASSED_BONUS = 10;
    private static final int XP_FLASHCARD_PER_CARD = 2;
    private static final int XP_FOCUS_SESSION = 15;

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private UserRepository userRepository;

    // ==================== XP & STATISTICHE ====================

    @Override
    @Transactional
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
    public UserStatsResponse getUserStatsResponse(UUID userId) {
        UserStats stats = getOrCreateUserStats(userId);
        long badgeCount = userBadgeRepository.countByUserId(userId);
        return UserStatsResponse.fromUserStats(stats, badgeCount);
    }

    @Override
    @Transactional
    public XpEventResponse recordExplanationXp(User user) {
        UserStats stats = getOrCreateUserStats(user.getId());

        // Aggiorna contatore e XP
        stats.incrementExplanations();
        stats.updateStreak();
        boolean leveledUp = stats.addXp(XP_EXPLANATION);

        userStatsRepository.save(stats);

        // Verifica badge
        List<Badge> newBadges = checkAndUnlockBadges(user, stats);

        logger.info("Utente {} ha guadagnato {} XP per spiegazione. Totale: {}",
                user.getEmail(), XP_EXPLANATION, stats.getTotalXp());

        return new XpEventResponse("EXPLANATION", XP_EXPLANATION, stats, leveledUp, newBadges);
    }

    @Override
    @Transactional
    public XpEventResponse recordQuizXp(User user, boolean passed) {
        UserStats stats = getOrCreateUserStats(user.getId());

        int xpEarned = XP_QUIZ_COMPLETED;
        if (passed) {
            xpEarned += XP_QUIZ_PASSED_BONUS;
        }

        stats.incrementQuizzesCompleted(passed);
        stats.updateStreak();
        boolean leveledUp = stats.addXp(xpEarned);

        userStatsRepository.save(stats);

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
    public XpEventResponse recordFocusSessionXp(User user, int durationMinutes) {
        UserStats stats = getOrCreateUserStats(user.getId());

        stats.incrementFocusSessions(durationMinutes);
        stats.updateStreak();
        boolean leveledUp = stats.addXp(XP_FOCUS_SESSION);

        userStatsRepository.save(stats);

        List<Badge> newBadges = checkAndUnlockBadges(user, stats);

        logger.info("Utente {} ha guadagnato {} XP per sessione focus. Totale: {}",
                user.getEmail(), XP_FOCUS_SESSION, stats.getTotalXp());

        return new XpEventResponse("FOCUS_SESSION", XP_FOCUS_SESSION, stats, leveledUp, newBadges);
    }

    // ==================== BADGE ====================

    @Override
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
    public List<BadgeResponse> getUnlockedBadges(UUID userId) {
        return userBadgeRepository.findByUserIdOrderByUnlockedAtDesc(userId)
                .stream()
                .map(BadgeResponse::fromUserBadge)
                .collect(Collectors.toList());
    }

    @Override
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

        double progress = (double) currentValue / badge.getRequirementValue() * 100;
        return Math.min(progress, 100.0);
    }

    // ==================== RACCOMANDAZIONI ====================

    @Override
    public List<RecommendationResponse> getActiveRecommendations(UUID userId) {
        List<Recommendation> active = recommendationRepository
                .findActiveByUserId(userId, LocalDateTime.now());
        return RecommendationResponse.fromList(active);
    }

    @Override
    @Transactional
    public List<Recommendation> generateRecommendations(User user) {
        List<Recommendation> newRecs = new ArrayList<>();
        UserStats stats = getOrCreateUserStats(user.getId());

        // 1. Mantieni lo streak
        if (stats.getCurrentStreak() > 0 &&
                stats.getLastActivityDate() != null &&
                stats.getLastActivityDate().isBefore(java.time.LocalDate.now())) {

            Recommendation streakRec = createRecommendation(
                    user,
                    RecommendationType.STREAK_REMINDER,
                    "Mantieni il tuo streak! 🔥",
                    "Hai uno streak di " + stats.getCurrentStreak() + " giorni. Non perderlo!",
                    null,
                    "Non perdere il tuo streak di studio",
                    Priority.HIGH
            );
            if (streakRec != null) newRecs.add(streakRec);
        }

        // 2. Obiettivo giornaliero XP
        if (stats.getWeeklyXp() < 50) {
            Recommendation dailyGoal = createRecommendation(
                    user,
                    RecommendationType.DAILY_GOAL,
                    "Raggiungi 50 XP oggi! 🎯",
                    "Completa qualche attività per raggiungere il tuo obiettivo giornaliero",
                    null,
                    "Guadagna XP per salire di livello",
                    Priority.MEDIUM
            );
            if (dailyGoal != null) newRecs.add(dailyGoal);
        }

        // 3. Suggerisci di provare nuove funzionalità
        if (stats.getQuizzesCompleted() == 0) {
            Recommendation tryQuiz = createRecommendation(
                    user,
                    RecommendationType.NEW_TOPIC,
                    "Prova a creare un Quiz! 📝",
                    "Genera un quiz con l'AI per testare le tue conoscenze",
                    null,
                    "Non hai ancora completato nessun quiz",
                    Priority.MEDIUM
            );
            if (tryQuiz != null) newRecs.add(tryQuiz);
        }

        if (stats.getFlashcardsStudied() == 0) {
            Recommendation tryFlashcards = createRecommendation(
                    user,
                    RecommendationType.STUDY_FLASHCARDS,
                    "Scopri le Flashcards! 🃏",
                    "Crea un deck di flashcards per memorizzare concetti importanti",
                    null,
                    "Non hai ancora studiato nessuna flashcard",
                    Priority.MEDIUM
            );
            if (tryFlashcards != null) newRecs.add(tryFlashcards);
        }

        return newRecs;
    }

    private Recommendation createRecommendation(User user, RecommendationType type,
                                                String title, String description,
                                                String topic, String reason, Priority priority) {
        // Verifica se esiste già una raccomandazione simile
        if (recommendationRepository.existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
                user.getId(), type, topic)) {
            return null;
        }

        Recommendation rec = new Recommendation();
        rec.setUser(user);
        rec.setType(type);
        rec.setTitle(title);
        rec.setDescription(description);
        rec.setTopic(topic);
        rec.setReason(reason);
        rec.setPriority(priority);
        rec.setExpiresAt(LocalDateTime.now().plusDays(1)); // Scade dopo 1 giorno

        return recommendationRepository.save(rec);
    }

    @Override
    @Transactional
    public void dismissRecommendation(UUID recommendationId, UUID userId) {
        Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Raccomandazione non trovata"));

        if (!rec.getUser().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        rec.dismiss();
        recommendationRepository.save(rec);
    }

    @Override
    @Transactional
    public void completeRecommendation(UUID recommendationId, UUID userId) {
        Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Raccomandazione non trovata"));

        if (!rec.getUser().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        rec.complete();
        recommendationRepository.save(rec);
    }

    // ==================== LEADERBOARD ====================

    @Override
    public List<LeaderboardEntry> getXpLeaderboard(int limit) {
        return userStatsRepository.findTopByTotalXp(limit)
                .stream()
                .map(stats -> new LeaderboardEntry(0, stats, "XP"))
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaderboardEntry> getWeeklyLeaderboard(int limit) {
        return userStatsRepository.findTopByWeeklyXp(limit)
                .stream()
                .map(stats -> new LeaderboardEntry(0, stats, "WEEKLY_XP"))
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaderboardEntry> getStreakLeaderboard(int limit) {
        return userStatsRepository.findTopByStreak(limit)
                .stream()
                .map(stats -> new LeaderboardEntry(0, stats, "STREAK"))
                .collect(Collectors.toList());
    }

    @Override
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