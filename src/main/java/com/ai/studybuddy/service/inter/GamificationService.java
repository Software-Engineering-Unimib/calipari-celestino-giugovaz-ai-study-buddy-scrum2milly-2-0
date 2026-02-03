package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.dto.gamification.GamificationDTO.*;
import com.ai.studybuddy.model.gamification.Badge;
import com.ai.studybuddy.model.gamification.Recommendation;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.user.User;

import java.util.List;
import java.util.UUID;

/**
 * Service per il sistema di gamification
 */
public interface GamificationService {

    // ==================== XP & STATISTICHE ====================

    /**
     * Ottiene o crea le statistiche dell'utente
     */
    UserStats getOrCreateUserStats(UUID userId);

    /**
     * Ottiene le statistiche formattate per la risposta API
     */
    UserStatsResponse getUserStatsResponse(UUID userId);

    /**
     * Registra XP per una spiegazione richiesta (+10 XP)
     */
    XpEventResponse recordExplanationXp(User user);

    /**
     * Registra XP per un quiz completato (+20 XP)
     */
    XpEventResponse recordQuizXp(User user, boolean passed);

    /**
     * Registra XP per flashcards studiate
     */
    XpEventResponse recordFlashcardXp(User user, int cardsStudied);

    /**
     * Registra XP per una sessione focus (+15 XP)
     */
    XpEventResponse recordFocusSessionXp(User user, int durationMinutes);

    // ==================== BADGE ====================

    /**
     * Ottiene tutti i badge disponibili con stato di sblocco
     */
    List<BadgeResponse> getAllBadgesWithStatus(UUID userId);

    /**
     * Ottiene solo i badge sbloccati dall'utente
     */
    List<BadgeResponse> getUnlockedBadges(UUID userId);

    /**
     * Ottiene i badge nuovi (non ancora visti)
     */
    List<BadgeResponse> getNewBadges(UUID userId);

    /**
     * Marca i badge come visti
     */
    void markBadgesAsSeen(UUID userId);

    /**
     * Verifica e sblocca eventuali nuovi badge
     */
    List<Badge> checkAndUnlockBadges(User user, UserStats stats);

    // ==================== RACCOMANDAZIONI ====================

    /**
     * Ottiene le raccomandazioni attive per l'utente
     */
    List<RecommendationResponse> getActiveRecommendations(UUID userId);

    /**
     * Genera nuove raccomandazioni basate sui progressi
     */
    List<Recommendation> generateRecommendations(User user);

    /**
     * Ignora una raccomandazione
     */
    void dismissRecommendation(UUID recommendationId, UUID userId);

    /**
     * Segna una raccomandazione come completata
     */
    void completeRecommendation(UUID recommendationId, UUID userId);

    // ==================== LEADERBOARD ====================

    /**
     * Ottiene la leaderboard per XP totale
     */
    List<LeaderboardEntry> getXpLeaderboard(int limit);

    /**
     * Ottiene la leaderboard per XP settimanale
     */
    List<LeaderboardEntry> getWeeklyLeaderboard(int limit);

    /**
     * Ottiene la leaderboard per streak
     */
    List<LeaderboardEntry> getStreakLeaderboard(int limit);

    /**
     * Ottiene la posizione dell'utente nella leaderboard
     */
    int getUserRank(UUID userId, String type);
}