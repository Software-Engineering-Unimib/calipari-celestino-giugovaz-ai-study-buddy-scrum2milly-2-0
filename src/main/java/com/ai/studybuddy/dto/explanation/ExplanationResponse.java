package com.ai.studybuddy.dto.explanation;

import com.ai.studybuddy.model.gamification.Badge;
import java.util.List;

/**
 * DTO per la risposta di una spiegazione AI con info gamification
 */
public class ExplanationResponse {

    private String topic;
    private String level;
    private String subject;
    private String explanation;

    // Gamification
    private int xpEarned;
    private int totalXp;
    private int newLevel;
    private boolean leveledUp;
    private List<Badge> newBadges;

    // Costruttori
    public ExplanationResponse() {}

    public ExplanationResponse(String topic, String level, String explanation) {
        this.topic = topic;
        this.level = level;
        this.explanation = explanation;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ExplanationResponse response = new ExplanationResponse();

        public Builder topic(String topic) {
            response.topic = topic;
            return this;
        }

        public Builder level(String level) {
            response.level = level;
            return this;
        }

        public Builder subject(String subject) {
            response.subject = subject;
            return this;
        }

        public Builder explanation(String explanation) {
            response.explanation = explanation;
            return this;
        }

        public Builder xpEarned(int xpEarned) {
            response.xpEarned = xpEarned;
            return this;
        }

        public Builder totalXp(int totalXp) {
            response.totalXp = totalXp;
            return this;
        }

        public Builder newLevel(int newLevel) {
            response.newLevel = newLevel;
            return this;
        }

        public Builder leveledUp(boolean leveledUp) {
            response.leveledUp = leveledUp;
            return this;
        }

        public Builder newBadges(List<Badge> newBadges) {
            response.newBadges = newBadges;
            return this;
        }

        public ExplanationResponse build() {
            return response;
        }
    }

    // Getters & Setters
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

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

    public int getNewLevel() {
        return newLevel;
    }

    public void setNewLevel(int newLevel) {
        this.newLevel = newLevel;
    }

    public boolean isLeveledUp() {
        return leveledUp;
    }

    public void setLeveledUp(boolean leveledUp) {
        this.leveledUp = leveledUp;
    }

    public List<Badge> getNewBadges() {
        return newBadges;
    }

    public void setNewBadges(List<Badge> newBadges) {
        this.newBadges = newBadges;
    }
}