package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.google.gson.JsonArray;

/**
 * Interfaccia per il servizio AI
 */
public interface AIService {

    /**
     * Genera spiegazione personalizzata
     */
    String generateExplanation(String topic, String studentLevel);

    /**
     * Genera quiz
     */
    String generateQuiz(String topic, int numQuestions, String difficulty);

    /**
     * Genera quiz con DifficultyLevel enum
     */
    String generateQuiz(String topic, int numQuestions, DifficultyLevel difficulty);

    /**
     * Genera flashcard (metodo legacy)
     * @deprecated Usa {@link #generateFlashcards(String, int, DifficultyLevel)} invece
     */
    @Deprecated
    String generateFlashCard(String topic, int numCards, String difficulty);

    /**
     * Genera flashcard
     */
    String generateFlashcards(String topic, int numCards, DifficultyLevel difficulty);

    /**
     * Genera flashcard con contesto aggiuntivo
     */
    String generateFlashcardsWithContext(String topic, int numCards, DifficultyLevel difficulty, String context);

    /**
     * Parsa la risposta JSON delle flashcards
     */
    JsonArray parseFlashcardsResponse(String aiResponse);
}
