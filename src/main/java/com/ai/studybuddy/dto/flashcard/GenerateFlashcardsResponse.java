package com.ai.studybuddy.dto.flashcard;

import com.ai.studybuddy.model.flashcard.Flashcard;
import java.util.List;

/**
 * Response DTO per la generazione di flashcards tramite AI
 */
public class GenerateFlashcardsResponse {

    private boolean success;
    private String message;
    private List<Flashcard> flashcards;

    // ==================== CAMPI GAMIFICATION ====================

    private int xpEarned;
    private int totalXp;
    private boolean leveledUp;

    // ==================== COSTRUTTORI ====================

    public GenerateFlashcardsResponse() {}

    public GenerateFlashcardsResponse(boolean success, String message, List<Flashcard> flashcards) {
        this.success = success;
        this.message = message;
        this.flashcards = flashcards;
    }

    // ==================== GETTERS & SETTERS ====================

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

    // ==================== GAMIFICATION GETTERS & SETTERS ====================

    public int getXpEarned() {
        return xpEarned;
    }

    public void setXpEarned(int xpEarned) {
        this.xpEarned = xpEarned;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public boolean isLeveledUp() {
        return leveledUp;
    }

    public void setLeveledUp(boolean leveledUp) {
        this.leveledUp = leveledUp;
    }
}