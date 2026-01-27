package com.ai.studybuddy.dto;

import com.ai.studybuddy.model.flashcard.Flashcard;
import java.util.List;

/**
 * Response DTO per la generazione di flashcards tramite AI
 */
public class GenerateFlashcardsResponse {
    
    private boolean success;
    private String message;
    private List<Flashcard> flashcards;

    // Costruttore
    public GenerateFlashcardsResponse(boolean success, String message, List<Flashcard> flashcards) {
        this.success = success;
        this.message = message;
        this.flashcards = flashcards;
    }

    // Getters e Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Flashcard> getFlashcards() {
        return flashcards;
    }

    public void setFlashcards(List<Flashcard> flashcards) {
        this.flashcards = flashcards;
    }
}
