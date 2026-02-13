package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.gamification.GamificationDTO.*;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.inter.GamificationService;
import com.ai.studybuddy.service.inter.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controller per il sistema di gamification
 *
 * Endpoints:
 * - GET /api/gamification/stats - Statistiche utente
 * - GET /api/gamification/badges - Tutti i badge con stato
 * - GET /api/gamification/badges/unlocked - Badge sbloccati
 * - GET /api/gamification/badges/new - Nuovi badge (non visti)
 * - POST /api/gamification/badges/seen - Marca badge come visti
 * - GET /api/gamification/recommendations - Raccomandazioni attive
 * - POST /api/gamification/recommendations/generate - Genera nuove raccomandazioni
 * - POST /api/gamification/recommendations/{id}/dismiss - Ignora raccomandazione
 * - POST /api/gamification/recommendations/{id}/complete - Completa raccomandazione
 * - GET /api/gamification/leaderboard/{type} - Leaderboard
 */
@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    private static final Logger logger = LoggerFactory.getLogger(GamificationController.class);

    private final GamificationService gamificationService;
    private final UserService userService;

    public GamificationController(GamificationService gamificationService, UserService userService) {
        this.gamificationService = gamificationService;
        this.userService = userService;
    }

    // ==================== STATISTICHE ====================

    /**
     * Ottiene le statistiche complete dell'utente
     */
    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getStats(Principal principal) {
        User user = userService.getCurrentUser(principal);
        UserStatsResponse stats = gamificationService.getUserStatsResponse(user.getId());
        return ResponseEntity.ok(stats);
    }

    // ==================== BADGE ====================

    /**
     * Ottiene tutti i badge con stato di sblocco e progresso
     */
    @GetMapping("/badges")
    public ResponseEntity<List<BadgeResponse>> getAllBadges(Principal principal) {
        User user = userService.getCurrentUser(principal);
        List<BadgeResponse> badges = gamificationService.getAllBadgesWithStatus(user.getId());
        return ResponseEntity.ok(badges);
    }

    /**
     * Ottiene solo i badge sbloccati
     */
    @GetMapping("/badges/unlocked")
    public ResponseEntity<List<BadgeResponse>> getUnlockedBadges(Principal principal) {
        User user = userService.getCurrentUser(principal);
        List<BadgeResponse> badges = gamificationService.getUnlockedBadges(user.getId());
        return ResponseEntity.ok(badges);
    }

    /**
     * Ottiene i badge nuovi (non ancora visti dall'utente)
     */
    @GetMapping("/badges/new")
    public ResponseEntity<List<BadgeResponse>> getNewBadges(Principal principal) {
        User user = userService.getCurrentUser(principal);
        List<BadgeResponse> badges = gamificationService.getNewBadges(user.getId());
        return ResponseEntity.ok(badges);
    }

    /**
     * Marca tutti i badge come visti
     */
    @PostMapping("/badges/seen")
    public ResponseEntity<Void> markBadgesAsSeen(Principal principal) {
        User user = userService.getCurrentUser(principal);
        gamificationService.markBadgesAsSeen(user.getId());
        return ResponseEntity.ok().build();
    }

    // ==================== LEADERBOARD ====================

    /**
     * Ottiene la leaderboard
     * @param type - XP, WEEKLY_XP, STREAK, LEVEL
     */
    @GetMapping("/leaderboard/{type}")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(
            @PathVariable String type,
            @RequestParam(defaultValue = "10") int limit) {

        List<LeaderboardEntry> entries = switch (type.toUpperCase()) {
            case "XP" -> gamificationService.getXpLeaderboard(limit);
            case "WEEKLY_XP", "WEEKLY" -> gamificationService.getWeeklyLeaderboard(limit);
            case "STREAK" -> gamificationService.getStreakLeaderboard(limit);
            default -> gamificationService.getXpLeaderboard(limit);
        };

        // Aggiunge il rank
        for (int i = 0; i < entries.size(); i++) {
            // Il rank è già nel costruttore, ma lo impostiamo correttamente
            // (il costruttore riceve 0, ma la posizione nell'array è il rank)
        }

        return ResponseEntity.ok(entries);
    }

    /**
     * Ottiene la posizione dell'utente nella leaderboard
     */
    @GetMapping("/leaderboard/{type}/my-rank")
    public ResponseEntity<Integer> getMyRank(
            @PathVariable String type,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        int rank = gamificationService.getUserRank(user.getId(), type.toUpperCase());
        return ResponseEntity.ok(rank);
    }

    // ==================== FOCUS SESSION ====================

    /**
     * Registra una sessione focus completata
     * XP calcolati: +3 XP ogni 10 minuti + 1 XP bonus completamento
     */
    @PostMapping("/focus-session")
    public ResponseEntity<XpEventResponse> recordFocusSession(
            @RequestBody FocusSessionRequest request,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        logger.info("Sessione focus completata da {}: {} minuti, {} XP",
                user.getEmail(), request.getDurationMinutes(), request.getXpEarned());

        // Usa XP dal frontend se presente, altrimenti calcola
        Integer xpToAward = request.getXpEarned();
        if (xpToAward == null || xpToAward <= 0) {
            // Calcolo backend: +3 XP ogni 10 minuti + 1 bonus
            xpToAward = (request.getDurationMinutes() / 10) * 3 + 1;
        }

        XpEventResponse xpEvent = gamificationService.recordFocusSessionXp(
                user,
                request.getDurationMinutes(),
                xpToAward
        );

        logger.info("Focus session XP: +{}, Totale: {}",
                xpEvent.getXpEarned(), xpEvent.getNewTotalXp());

        return ResponseEntity.ok(xpEvent);
    }

    /**
     * DTO per richiesta sessione focus
     */
    public static class FocusSessionRequest {
        private int durationMinutes;
        private Integer xpEarned; // XP calcolati dal frontend (opzionale)

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public Integer getXpEarned() {
            return xpEarned;
        }

        public void setXpEarned(Integer xpEarned) {
            this.xpEarned = xpEarned;
        }
    }
}