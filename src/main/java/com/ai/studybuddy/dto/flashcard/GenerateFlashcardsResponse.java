package com.ai.studybuddy.dto.flashcard;

import com.ai.studybuddy.model.flashcard.Flashcard;
import java.util.Collections;
import java.util.List;

/**
 * Response DTO per la generazione di flashcards tramite AI
 */
public class GenerateFlashcardsResponse {

    private boolean success;
    private String message;
    private List<Flashcard> flashcards;
    private int totalGenerated;

    // Costruttore privato per factory methods
    private GenerateFlashcardsResponse() {}

    // Costruttore pubblico
    public GenerateFlashcardsResponse(boolean success, String message, List<Flashcard> flashcards) {
        this.success = success;
        this.message = message;
        this.flashcards = flashcards != null ? flashcards : Collections.emptyList();
        this.totalGenerated = this.flashcards.size();
    }

    // Factory methods
    public static GenerateFlashcardsResponse success(List<Flashcard> flashcards) {
        return new GenerateFlashcardsResponse(
                true,
                String.format("Generate %d flashcards con successo", flashcards.size()),
                flashcards
        );
    }

    public static GenerateFlashcardsResponse success(String message, List<Flashcard> flashcards) {
        return new GenerateFlashcardsResponse(true, message, flashcards);
    }

    public static GenerateFlashcardsResponse error(String message) {
        return new GenerateFlashcardsResponse(false, message, Collections.emptyList());
    }

    public static GenerateFlashcardsResponse partialSuccess(String message, List<Flashcard> flashcards) {
        GenerateFlashcardsResponse response = new GenerateFlashcardsResponse();
        response.success = true;
        response.message = message;
        response.flashcards = flashcards;
        response.totalGenerated = flashcards.size();
        return response;
    }

    // Utility methods
    public boolean hasFlashcards() {
        return flashcards != null && !flashcards.isEmpty();
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
        this.totalGenerated = flashcards != null ? flashcards.size() : 0;
    }

    public int getTotalGenerated() {
        return totalGenerated;
    }
}