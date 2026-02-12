package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.explanation.ExplanationResponse;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.inter.ExplanationService;
import com.ai.studybuddy.service.inter.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class ExplanationController {

    private static final Logger logger = LoggerFactory.getLogger(ExplanationController.class);

    private final ExplanationService explanationService;
    private final UserService userService;

    public ExplanationController(ExplanationService explanationService,
                                 UserService userService) {
        this.explanationService = explanationService;
        this.userService = userService;
    }

    // ==================== EXPLANATION ====================

    /**
     * Genera una spiegazione personalizzata
     * ASSEGNA XP PER SPIEGAZIONE (+10 XP)
     */
    @GetMapping("/explain")
    public ResponseEntity<ExplanationResponse> getExplanation(
            @RequestParam String topic,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String subject,
            Principal principal) {

        User user = userService.getCurrentUser(principal);

        String educationLevel = level;
        if (educationLevel == null || educationLevel.isBlank()) {
            educationLevel = user.getEducationLevel() != null
                    ? user.getEducationLevel().getDisplayName()
                    : "Università";
        }

        logger.info("Richiesta spiegazione '{}' da utente: {} - Livello: {}, Lingua: {}",
                topic, user.getEmail(), educationLevel, user.getPreferredLanguage());

        ExplanationResponse response = explanationService.generateExplanation(
                topic, educationLevel, subject, user);

        logger.info("Spiegazione generata - XP: +{}, Totale: {}",
                response.getXpEarned(), response.getTotalXp());

        return ResponseEntity.ok(response);
    }

    // ==================== DEBUG / UTILITY ====================

    @GetMapping("/debug/user-info")
    public ResponseEntity<Map<String, Object>> debugUserInfo(Principal principal) {
        User user = userService.getCurrentUser(principal);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("preferredLanguage", user.getPreferredLanguage());
        response.put("educationLevel", user.getEducationLevel());
        response.put("fullName", user.getFullName());
        response.put("streakDays", user.getStreakDays());

        return ResponseEntity.ok(response);
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}