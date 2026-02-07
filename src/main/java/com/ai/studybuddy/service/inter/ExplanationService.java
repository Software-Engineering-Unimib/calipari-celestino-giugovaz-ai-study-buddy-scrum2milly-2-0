package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.dto.explanation.ExplanationResponse;
import com.ai.studybuddy.model.user.User;

/**
 * Service per la gestione delle spiegazioni AI
 */
public interface ExplanationService {

    /**
     * Genera una spiegazione personalizzata per un argomento
     *
     * @param topic argomento da spiegare
     * @param level livello dello studente (scuola_media, scuola_superiore, università, esperto)
     * @param subject materia di riferimento (opzionale)
     * @param user utente che richiede la spiegazione
     * @return ExplanationResponse con spiegazione e info XP
     */
    ExplanationResponse generateExplanation(String topic, String level, String subject, User user);

    /**
     * Genera una spiegazione senza tracciamento XP (per preview o test)
     */
    String generateExplanationPreview(String topic, String level);

}