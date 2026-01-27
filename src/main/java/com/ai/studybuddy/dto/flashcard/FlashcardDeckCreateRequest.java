package com.ai.studybuddy.dto.flashcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO per creare un nuovo deck di flashcards
 */
public class FlashcardDeckCreateRequest {

    @NotBlank(message = "Il nome del deck è obbligatorio")
    @Size(min = 1, max = 100, message = "Il nome deve essere tra 1 e 100 caratteri")
    private String name;

    @Size(max = 500, message = "La descrizione non può superare 500 caratteri")
    private String description;

    @Size(max = 100, message = "La materia non può superare 100 caratteri")
    private String subject;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "Il colore deve essere un codice hex valido (es: #3B82F6)")
    private String color = "#3B82F6";

    @Size(max = 50, message = "L'icona non può superare 50 caratteri")
    private String icon;

    private Boolean isPublic = false;

    // Costruttori
    public FlashcardDeckCreateRequest() {}

    public FlashcardDeckCreateRequest(String name) {
        this.name = name;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private String subject;
        private String color = "#3B82F6";
        private String icon;
        private Boolean isPublic = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder isPublic(Boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public FlashcardDeckCreateRequest build() {
            FlashcardDeckCreateRequest request = new FlashcardDeckCreateRequest();
            request.setName(this.name);
            request.setDescription(this.description);
            request.setSubject(this.subject);
            request.setColor(this.color);
            request.setIcon(this.icon);
            request.setIsPublic(this.isPublic);
            return request;
        }
    }

    // Getters & Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
}