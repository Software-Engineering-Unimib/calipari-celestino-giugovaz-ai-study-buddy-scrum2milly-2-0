package com.ai.studybuddy.dto.quiz;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * DTO per inviare le risposte di un quiz
 */
public class QuizAnswerRequest {

    @NotNull(message = "L'ID del quiz è obbligatorio")
    private UUID quizId;

    @NotEmpty(message = "Le risposte sono obbligatorie")
    private Map<UUID, String> answers;  // questionId -> "A", "B", "C" o "D"

    // Costruttori
    public QuizAnswerRequest() {}

    public QuizAnswerRequest(UUID quizId, Map<UUID, String> answers) {
        this.quizId = quizId;
        this.answers = answers;
    }

    // Getters & Setters
    public UUID getQuizId() {
        return quizId;
    }

    public void setQuizId(UUID quizId) {
        this.quizId = quizId;
    }

    public Map<UUID, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<UUID, String> answers) {
        this.answers = answers;
    }
}