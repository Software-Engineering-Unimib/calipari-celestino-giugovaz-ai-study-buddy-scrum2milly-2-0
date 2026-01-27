package com.ai.studybuddy.dto.quiz;

import com.ai.studybuddy.util.enums.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO per generare un quiz con AI
 */
public class QuizGenerateRequest {

    @NotBlank(message = "Il topic è obbligatorio")
    @Size(max = 200, message = "Il topic non può superare 200 caratteri")
    private String topic;

    @Size(max = 100, message = "La materia non può superare 100 caratteri")
    private String subject;

    @Min(value = 1, message = "Il numero minimo di domande è 1")
    @Max(value = 20, message = "Il numero massimo di domande è 20")
    private Integer numberOfQuestions = 5;

    private DifficultyLevel difficultyLevel = DifficultyLevel.INTERMEDIO;

    @Size(max = 500, message = "Il contesto non può superare 500 caratteri")
    private String context;

    // Costruttori
    public QuizGenerateRequest() {}

    public QuizGenerateRequest(String topic, Integer numberOfQuestions, DifficultyLevel difficultyLevel) {
        this.topic = topic;
        this.numberOfQuestions = numberOfQuestions;
        this.difficultyLevel = difficultyLevel;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String topic;
        private String subject;
        private Integer numberOfQuestions = 5;
        private DifficultyLevel difficultyLevel = DifficultyLevel.INTERMEDIO;
        private String context;

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder numberOfQuestions(Integer numberOfQuestions) {
            this.numberOfQuestions = numberOfQuestions;
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

        public QuizGenerateRequest build() {
            QuizGenerateRequest request = new QuizGenerateRequest();
            request.setTopic(this.topic);
            request.setSubject(this.subject);
            request.setNumberOfQuestions(this.numberOfQuestions);
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
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