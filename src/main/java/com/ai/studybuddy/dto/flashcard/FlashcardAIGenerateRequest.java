package com.ai.studybuddy.dto.flashcard;

import com.ai.studybuddy.util.enums.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO per generare flashcards con AI
 */
public class FlashcardAIGenerateRequest {

    @NotBlank(message = "Il topic è obbligatorio")
    @Size(max = 200, message = "Il topic non può superare 200 caratteri")
    private String topic;

    @Min(value = 1, message = "Il numero minimo di carte è 1")
    @Max(value = 20, message = "Il numero massimo di carte è 20")
    private Integer numberOfCards = 5;

    private DifficultyLevel difficultyLevel = DifficultyLevel.INTERMEDIO;

    @Size(max = 500, message = "Il contesto non può superare 500 caratteri")
    private String context;

    // Costruttori
    public FlashcardAIGenerateRequest() {}

    public FlashcardAIGenerateRequest(String topic, Integer numberOfCards, DifficultyLevel difficultyLevel) {
        this.topic = topic;
        this.numberOfCards = numberOfCards;
        this.difficultyLevel = difficultyLevel;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String topic;
        private Integer numberOfCards = 5;
        private DifficultyLevel difficultyLevel = DifficultyLevel.INTERMEDIO;
        private String context;

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder numberOfCards(Integer numberOfCards) {
            this.numberOfCards = numberOfCards;
            return this;
        }

        public Builder difficultyLevel(DifficultyLevel difficultyLevel) {
            this.difficultyLevel = difficultyLevel;
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public FlashcardAIGenerateRequest build() {
            FlashcardAIGenerateRequest request = new FlashcardAIGenerateRequest();
            request.setTopic(this.topic);
            request.setNumberOfCards(this.numberOfCards);
            request.setDifficultyLevel(this.difficultyLevel);
            request.setContext(this.context);
            return request;
        }
    }

    // Getters & Setters
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getNumberOfCards() {
        return numberOfCards;
    }

    public void setNumberOfCards(Integer numberOfCards) {
        this.numberOfCards = numberOfCards;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}