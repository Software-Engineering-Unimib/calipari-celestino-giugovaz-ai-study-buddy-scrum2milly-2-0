package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.google.gson.JsonArray;

/**
 * Interfaccia per il servizio AI
 */
public interface AIService {

    // ==================== NUOVI METODI CON SUPPORTO LINGUA ====================

    /**
     * Genera spiegazione personalizzata con supporto lingua
     */
    String generateExplanation(String topic, String studentLevel, String language);

    /**
     * Genera quiz con supporto lingua
     */
    String generateQuiz(String topic, int numQuestions, String difficulty, String language);

    /**
     * Genera quiz con DifficultyLevel enum e supporto lingua
     */
    String generateQuiz(String topic, int numQuestions, DifficultyLevel difficulty, String language);

    /**
     * Genera flashcard con supporto lingua
     */
    String generateFlashcards(String topic, int numCards, DifficultyLevel difficulty, String language);

    /**
     * Genera flashcard con contesto aggiuntivo e supporto lingua
     */
    String generateFlashcardsWithContext(String topic, int numCards, DifficultyLevel difficulty, 
                                         String context, String language);

    // ==================== METODI LEGACY (per retrocompatibilità) ====================

    /**
     * Genera spiegazione personalizzata (legacy - italiano di default)
     * @deprecated Usa {@link #generateExplanation(String, String, String)} invece
     */
    @Deprecated
    default String generateExplanation(String topic, String studentLevel) {
        return generateExplanation(topic, studentLevel, "it");
    }

    /**
     * Genera quiz (legacy - italiano di default)
     * @deprecated Usa {@link #generateQuiz(String, int, String, String)} invece
     */
    @Deprecated
    default String generateQuiz(String topic, int numQuestions, String difficulty) {
        return generateQuiz(topic, numQuestions, difficulty, "it");
    }

    /**
     * Genera quiz con DifficultyLevel enum (legacy - italiano di default)
     * @deprecated Usa {@link #generateQuiz(String, int, DifficultyLevel, String)} invece
     */
    @Deprecated
    default String generateQuiz(String topic, int numQuestions, DifficultyLevel difficulty) {
        return generateQuiz(topic, numQuestions, difficulty, "it");
    }

    /**
     * Genera flashcard (legacy)
     * @deprecated Usa {@link #generateFlashcards(String, int, DifficultyLevel, String)} invece
     */
    @Deprecated
    default String generateFlashCard(String topic, int numCards, String difficulty) {
        DifficultyLevel level = DifficultyLevel.fromString(difficulty);
        return generateFlashcards(topic, numCards, level, "it");
    }

    /**
     * Genera flashcard (legacy - italiano di default)
     * @deprecated Usa {@link #generateFlashcards(String, int, DifficultyLevel, String)} invece
     */
    @Deprecated
    default String generateFlashcards(String topic, int numCards, DifficultyLevel difficulty) {
        return generateFlashcards(topic, numCards, difficulty, "it");
    }

    /**
     * Genera flashcard con contesto aggiuntivo (legacy - italiano di default)
     * @deprecated Usa {@link #generateFlashcardsWithContext(String, int, DifficultyLevel, String, String)} invece
     */
    @Deprecated
    default String generateFlashcardsWithContext(String topic, int numCards, DifficultyLevel difficulty, String context) {
        return generateFlashcardsWithContext(topic, numCards, difficulty, context, "it");
    }

    // ==================== METODI UTILITY ====================

    /**
     * Parsa la risposta JSON delle flashcards
     */
    JsonArray parseFlashcardsResponse(String aiResponse);

    /**
     * Verifica se almeno un modello AI è disponibile
     */
    boolean isAnyModelAvailable();

    /**
     * Ottiene il nome del modello AI disponibile
     */
    String getAvailableModel();
}