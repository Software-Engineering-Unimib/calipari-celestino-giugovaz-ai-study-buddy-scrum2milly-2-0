package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.explanation.ExplanationResponse;
import com.ai.studybuddy.dto.gamification.GamificationDTO.XpEventResponse;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.inter.AIService;
import com.ai.studybuddy.service.inter.ExplanationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExplanationServiceImpl implements ExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ExplanationServiceImpl.class);

    private final AIService aiService;
    private final GamificationServiceImpl gamificationService;

    public ExplanationServiceImpl(AIService aiService,
                                  GamificationServiceImpl gamificationService) {
        this.aiService = aiService;
        this.gamificationService = gamificationService;
    }

    @Override
    public ExplanationResponse generateExplanation(String topic, String level, String subject, User user) {
        log.info("Generazione spiegazione - topic: '{}', level: '{}', user: {}",
                topic, level, user.getEmail());

        // ✅ PASSA LA LINGUA DELL'UTENTE!
        String language = user.getPreferredLanguage();  // Mai null (default "it" in User)
        String explanation = aiService.generateExplanation(topic, mapLevel(level), language);

        // Registra XP (+10 per spiegazione)
        XpEventResponse xpEvent = gamificationService.recordExplanationXp(user, topic, subject);

        log.info("Spiegazione generata per '{}' in lingua '{}' - XP guadagnati: {}, Totale: {}",
                topic, language, xpEvent.getXpEarned(), xpEvent.getNewTotalXp());

        return ExplanationResponse.builder()
                .topic(topic)
                .level(level)
                .subject(subject)
                .explanation(explanation)
                .xpEarned(xpEvent.getXpEarned())
                .totalXp(xpEvent.getNewTotalXp())
                .newLevel(xpEvent.getNewLevel())
                .leveledUp(xpEvent.isLeveledUp())
                .newBadges(xpEvent.getNewBadges())
                .build();
    }

    @Override
    public String generateExplanationPreview(String topic, String level) {
        log.info("Generazione preview spiegazione - topic: '{}', level: '{}'", topic, level);
        // Preview usa italiano (senza XP)
        return aiService.generateExplanation(topic, mapLevel(level), "it");
    }

    private String mapLevel(String level) {
        if (level == null) return "università";
        return switch (level.toLowerCase()) {
            case "scuola_media", "scuolamedia", "media" -> "scuola media";
            case "scuola_superiore", "scuolasuperiore", "superiore" -> "scuola superiore";
            case "università", "universita", "uni" -> "università";
            case "esperto", "expert", "avanzato" -> "esperto";
            default -> level;
        };
    }
}