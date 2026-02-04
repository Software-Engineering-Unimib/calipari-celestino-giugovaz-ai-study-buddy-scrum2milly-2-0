package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.dto.gamification.GamificationDTO.*;
import com.ai.studybuddy.model.gamification.Badge;
import com.ai.studybuddy.model.gamification.Recommendation;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.user.User;

import java.util.List;
import java.util.UUID;

/**
 * Service interface per il sistema di gamification
 *
 * XP Rewards:
 * - Spiegazione richiesta: +10 XP
 * - Quiz completato: +20 XP
 * - Quiz superato (bonus): +10 XP
 * - Flashcard studiata: +2 XP per card
 * - Sessione focus: +15 XP
 */
public interface GamificationService {

    // ==================== USER STATS ====================

    /**
     * Ottiene o crea le statistiche dell'utente
     */
    UserStats getOrCreateUserStats(UUID userId);

    /**
     * Ottiene le statistiche formattate per la risposta API
     */
    UserStatsResponse getUserStatsResponse(UUID userId);

    // ==================== XP TRACKING ====================

    /**
     * Registra XP per spiegazione richiesta (+10 XP)
     */
    XpEventResponse recordExplanationXp(User user);

    /**
     * Registra XP per quiz completato (+20 XP base, +10 bonus se superato)
     */
    XpEventResponse recordQuizXp(User user, boolean passed);

    /**
     * Registra XP per flashcards studiate (+2 XP per card)
     */
    XpEventResponse recordFlashcardXp(User user, int cardsStudied);

    /**
     * Registra XP per sessione focus completata (+15 XP)
     */
    XpEventResponse recordFocusSessionXp(User user, int durationMinutes);

    // ==================== BADGES ====================

    /**
     * Ottiene tutti i badge con stato di sblocco e progresso
     */
    List<BadgeResponse> getAllBadgesWithStatus(UUID userId);

    /**
     * Ottiene solo i badge sbloccati
     */
    List<BadgeResponse> getUnlockedBadges(UUID userId);

    /**
     * Ottiene i badge non ancora visti dall'utente
     */
    List<BadgeResponse> getNewBadges(UUID userId);

    /**
     * Marca tutti i badge come visti
     */
    void markBadgesAsSeen(UUID userId);

    /**
     * Verifica e sblocca badge in base alle statistiche
     */
    List<Badge> checkAndUnlockBadges(User user, UserStats stats);

    // ==================== RECOMMENDATIONS ====================

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
     * Ottiene la classifica per XP totali
     */
    List<LeaderboardEntry> getXpLeaderboard(int limit);

    /**
     * Ottiene la classifica per XP settimanali
     */
    List<LeaderboardEntry> getWeeklyLeaderboard(int limit);

    /**
     * Ottiene la classifica per streak
     */
    List<LeaderboardEntry> getStreakLeaderboard(int limit);

    /**
     * Ottiene la posizione dell'utente nella classifica
     */
    int getUserRank(UUID userId, String type);
}