package com.ai.studybuddy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entità Flashcard - rappresenta una singola scheda di studio
 */
@Entity
@Table(name = "flashcards")
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ==================== RELAZIONI ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    @JsonIgnore
    private FlashcardDeck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    @JsonIgnore
    private User createdBy;

    // ==================== CONTENUTO ====================
    
    @Column(name = "front_content", nullable = false, columnDefinition = "TEXT")
    private String frontContent;  // Domanda/Concetto

    @Column(name = "back_content", nullable = false, columnDefinition = "TEXT")
    private String backContent;   // Risposta/Spiegazione

    @Column(name = "hint", columnDefinition = "TEXT")
    private String hint;  // Suggerimento opzionale

    @Column(name = "tags", length = 500)
    private String tags;  // es: "matematica,algebra,equazioni" (separati da virgola)

    // ==================== METADATI ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level")
    private DifficultyLevel difficultyLevel;

    @Column(name = "ai_generated")
    private Boolean aiGenerated = false;  // true se creata dall'AI

    @Column(name = "source", length = 255)
    private String source;  // Riferimento (es: "Capitolo 5, pagina 42")

    // ==================== STATISTICHE ====================
    
    @Column(name = "times_reviewed")
    private Integer timesReviewed = 0;

    @Column(name = "times_correct")
    private Integer timesCorrect = 0;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    // ==================== AUDIT ====================
    
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== LIFECYCLE ====================
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== BUSINESS LOGIC ====================
    
    /**
     * Calcola la percentuale di successo
     */
    public double getSuccessRate() {
        if (timesReviewed == 0) return 0.0;
        return (double) timesCorrect / timesReviewed * 100;
    }

    /**
     * Registra una revisione
     */
    public void recordReview(boolean wasCorrect) {
        timesReviewed++;
        if (wasCorrect) {
            timesCorrect++;
        }
        lastReviewedAt = LocalDateTime.now();
    }

    /**
     * Ottiene i tag come array
     */
    public String[] getTagsArray() {
        if (tags == null || tags.isEmpty()) {
            return new String[0];
        }
        return tags.split(",");
    }

    /**
     * Imposta i tag da array
     */
    public void setTagsFromArray(String[] tagsArray) {
        if (tagsArray == null || tagsArray.length == 0) {
            this.tags = "";
        } else {
            this.tags = String.join(",", tagsArray);
        }
    }

    // ==================== GETTERS & SETTERS ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FlashcardDeck getDeck() {
        return deck;
    }

    public void setDeck(FlashcardDeck deck) {
        this.deck = deck;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public Boolean getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getTimesReviewed() {
        return timesReviewed;
    }

    public void setTimesReviewed(Integer timesReviewed) {
        this.timesReviewed = timesReviewed;
    }

    public Integer getTimesCorrect() {
        return timesCorrect;
    }

    public void setTimesCorrect(Integer timesCorrect) {
        this.timesCorrect = timesCorrect;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
