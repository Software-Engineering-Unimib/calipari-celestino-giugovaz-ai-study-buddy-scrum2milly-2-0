package com.ai.studybuddy.dto;

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

/**
 * DTO per registrare una revisione di flashcard
 */
class FlashcardReviewRequest {

    private Boolean wasCorrect;

    public Boolean getWasCorrect() {
        return wasCorrect;
    }

    public void setWasCorrect(Boolean wasCorrect) {
        this.wasCorrect = wasCorrect;
    }
}

/**
 * DTO per generare flashcards con AI
 */
class FlashcardAIGenerateRequest {

    @NotBlank(message = "Il topic è obbligatorio")
    private String topic;

    private Integer numberOfCards = 5;

    private String difficultyLevel;

    @Size(max = 500, message = "Il contesto non può superare 500 caratteri")
    private String context;

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

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
