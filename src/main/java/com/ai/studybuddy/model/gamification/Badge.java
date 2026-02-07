package com.ai.studybuddy.model.gamification;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entità Badge - rappresenta un achievement/traguardo sbloccabile
 */
@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;  // es: "CURIOSO", "QUIZ_MASTER"

    @Column(name = "name", nullable = false, length = 100)
    private String name;  // es: "Curioso", "Quiz Master"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;  // es: "Richiedi 10 spiegazioni"

    @Column(name = "icon", length = 50)
    private String icon;  // es: "bi-lightbulb", "bi-trophy"

    @Column(name = "color", length = 7)
    private String color = "#6366F1";  // Colore hex per UI

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private BadgeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", length = 20)
    private BadgeRarity rarity = BadgeRarity.COMMON;

    // Requisiti per sbloccare
    @Column(name = "requirement_type", length = 50)
    private String requirementType;  // es: "EXPLANATIONS_COUNT", "QUIZZES_COMPLETED"

    @Column(name = "requirement_value")
    private Integer requirementValue;  // es: 10, 50

    @Column(name = "xp_reward")
    private Integer xpReward = 0;  // XP bonus quando sbloccato

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==================== ENUMS ====================

    public enum BadgeCategory {
        STUDY,      // Studio generale
        QUIZ,       // Quiz
        FLASHCARD,  // Flashcards
        STREAK,     // Continuità
        SPECIAL     // Speciali
    }

    public enum BadgeRarity {
        COMMON,     // Comune
        UNCOMMON,   // Non comune
        RARE,       // Raro
        EPIC,       // Epico
        LEGENDARY   // Leggendario
    }

    // ==================== GETTERS & SETTERS ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BadgeCategory getCategory() {
        return category;
    }

    public void setCategory(BadgeCategory category) {
        this.category = category;
    }

    public BadgeRarity getRarity() {
        return rarity;
    }

    public void setRarity(BadgeRarity rarity) {
        this.rarity = rarity;
    }

    public String getRequirementType() {
        return requirementType;
    }

    public void setRequirementType(String requirementType) {
        this.requirementType = requirementType;
    }

    public Integer getRequirementValue() {
        return requirementValue;
    }

    public void setRequirementValue(Integer requirementValue) {
        this.requirementValue = requirementValue;
    }

    public Integer getXpReward() {
        return xpReward;
    }

    public void setXpReward(Integer xpReward) {
        this.xpReward = xpReward;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}