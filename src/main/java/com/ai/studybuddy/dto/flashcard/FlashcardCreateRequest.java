package com.ai.studybuddy.dto.flashcard;

import com.ai.studybuddy.util.enums.DifficultyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO per creare una nuova flashcard
 */
public class FlashcardCreateRequest {

    @NotBlank(message = "Il contenuto frontale è obbligatorio")
    @Size(max = 1000, message = "Il contenuto frontale non può superare 1000 caratteri")
    private String frontContent;

    @NotBlank(message = "Il contenuto posteriore è obbligatorio")
    @Size(max = 2000, message = "Il contenuto posteriore non può superare 2000 caratteri")
    private String backContent;

    @Size(max = 500, message = "Il suggerimento non può superare 500 caratteri")
    private String hint;

    private String[] tags;

    private DifficultyLevel difficultyLevel;

    @Size(max = 255, message = "La fonte non può superare 255 caratteri")
    private String source;

    // Costruttori
    public FlashcardCreateRequest() {}

    public FlashcardCreateRequest(String frontContent, String backContent) {
        this.frontContent = frontContent;
        this.backContent = backContent;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String frontContent;
        private String backContent;
        private String hint;
        private String[] tags;
        private DifficultyLevel difficultyLevel;
        private String source;

        public Builder frontContent(String frontContent) {
            this.frontContent = frontContent;
            return this;
        }

        public Builder backContent(String backContent) {
            this.backContent = backContent;
            return this;
        }

        public Builder hint(String hint) {
            this.hint = hint;
            return this;
        }

        public Builder tags(String... tags) {
            this.tags = tags;
            return this;
        }

        public Builder difficultyLevel(DifficultyLevel difficultyLevel) {
            this.difficultyLevel = difficultyLevel;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public FlashcardCreateRequest build() {
            FlashcardCreateRequest request = new FlashcardCreateRequest();
            request.setFrontContent(this.frontContent);
            request.setBackContent(this.backContent);
            request.setHint(this.hint);
            request.setTags(this.tags);
            request.setDifficultyLevel(this.difficultyLevel);
            request.setSource(this.source);
            return request;
        }
    }

    // Getters & Setters
    public String getFrontContent() {
        return frontContent;
    }

    public void setFrontContent(String frontContent) {
        this.frontContent = frontContent;
    }

    public String getBackContent() {
        return backContent;
    }

    public void setBackContent(String backContent) {
        this.backContent = backContent;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}