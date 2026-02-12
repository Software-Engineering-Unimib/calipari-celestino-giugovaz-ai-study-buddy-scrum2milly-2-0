package com.ai.studybuddy.dto.flashcard;

import com.ai.studybuddy.util.enums.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO per generare flashcards con AI
 * Supporto multilingua incluso
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

    // ⭐ CAMPO LINGUA (default italiano)
    @Size(min = 2, max = 10, message = "La lingua deve essere tra 2 e 10 caratteri")
    private String language = "it";

    // Costruttori
    public FlashcardAIGenerateRequest() {}

    public FlashcardAIGenerateRequest(String topic, Integer numberOfCards, 
                                     DifficultyLevel difficultyLevel) {
        this(topic, numberOfCards, difficultyLevel, "it");
    }

    public FlashcardAIGenerateRequest(String topic, Integer numberOfCards, 
                                     DifficultyLevel difficultyLevel, String language) {
        this.topic = topic;
        this.numberOfCards = numberOfCards;
        this.difficultyLevel = difficultyLevel;
        this.language = language != null ? language : "it";
    }

    // Builder pattern con supporto lingua
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String topic;
        private Integer numberOfCards = 5;
        private DifficultyLevel difficultyLevel = DifficultyLevel.INTERMEDIO;
        private String context;
        private String language = "it";

        public Builder topic(String topic) { this.topic = topic; return this; }
        public Builder numberOfCards(Integer numberOfCards) { 
            this.numberOfCards = numberOfCards; return this; 
        }
        public Builder difficultyLevel(DifficultyLevel difficultyLevel) { 
            this.difficultyLevel = difficultyLevel; return this; 
        }
        public Builder context(String context) { this.context = context; return this; }
        public Builder language(String language) { 
            this.language = language; return this; 
        }

        public FlashcardAIGenerateRequest build() {
            FlashcardAIGenerateRequest request = new FlashcardAIGenerateRequest();
            request.setTopic(this.topic);
            request.setNumberOfCards(this.numberOfCards);
            request.setDifficultyLevel(this.difficultyLevel);
            request.setContext(this.context);
            request.setLanguage(this.language);
            return request;
        }
    }

    // ==================== GETTERS & SETTERS ====================

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Integer getNumberOfCards() { return numberOfCards; }
    public void setNumberOfCards(Integer numberOfCards) { 
        this.numberOfCards = numberOfCards; 
    }

    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(DifficultyLevel difficultyLevel) { 
        this.difficultyLevel = difficultyLevel; 
    }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getLanguage() { 
        return language != null ? language : "it"; 
    }
    public void setLanguage(String language) { 
        this.language = language; 
    }

    // ⭐ Metodi utility
    public boolean hasContext() {
        return context != null && !context.trim().isEmpty();
    }

    public boolean hasLanguage() {
        return language != null && !language.isEmpty();
    }
}