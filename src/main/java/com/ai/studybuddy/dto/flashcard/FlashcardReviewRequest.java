package com.ai.studybuddy.dto.flashcard;

import jakarta.validation.constraints.NotNull;

/**
 * DTO per registrare una revisione di flashcard
 */
public class FlashcardReviewRequest {

    @NotNull(message = "Il campo wasCorrect è obbligatorio")
    private Boolean wasCorrect;

    // Costruttori
    public FlashcardReviewRequest() {}

    public FlashcardReviewRequest(Boolean wasCorrect) {
        this.wasCorrect = wasCorrect;
    }

    // Getter & Setter
    public Boolean getWasCorrect() {
        return wasCorrect;
    }

    public void setWasCorrect(Boolean wasCorrect) {
        this.wasCorrect = wasCorrect;
    }
}