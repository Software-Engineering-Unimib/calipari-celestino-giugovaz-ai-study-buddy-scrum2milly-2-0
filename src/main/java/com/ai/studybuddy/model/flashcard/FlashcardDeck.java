package com.ai.studybuddy.model.flashcard;

import com.ai.studybuddy.model.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * FlashcardDeck - rappresenta un mazzo/collezione di flashcards
 */
@Entity
@Table(name = "flashcard_decks", indexes = {
        @Index(name = "idx_deck_owner", columnList = "user_id"),
        @Index(name = "idx_deck_active", columnList = "is_active"),
        @Index(name = "idx_deck_public", columnList = "is_public"),
        @Index(name = "idx_deck_subject", columnList = "subject")
})
public class FlashcardDeck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ==================== RELAZIONI ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User owner;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Flashcard> flashcards = new ArrayList<>();

    // ==================== DATI MAZZO ====================

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "subject", length = 100)
    private String subject;

    @Column(name = "color", length = 7)
    private String color = "#3B82F6";

    @Column(name = "icon", length = 50)
    private String icon;

    // ==================== VISIBILITÀ ====================

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "is_shared")
    private Boolean isShared = false;

    // ==================== STATISTICHE ====================

    @Column(name = "total_cards")
    private Integer totalCards = 0;

    @Column(name = "cards_mastered")
    private Integer cardsMastered = 0;

    @Column(name = "times_studied")
    private Integer timesStudied = 0;

    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt;

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
        if (totalCards == null) totalCards = 0;
        if (cardsMastered == null) cardsMastered = 0;
        if (timesStudied == null) timesStudied = 0;
        if (isActive == null) isActive = true;
        if (isPublic == null) isPublic = false;
        if (isShared == null) isShared = false;
        if (color == null) color = "#3B82F6";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Aggiunge una flashcard al deck
     */
    public void addFlashcard(Flashcard flashcard) {
        flashcards.add(flashcard);
        flashcard.setDeck(this);
        totalCards = flashcards.size();
    }

    /**
     * Rimuove una flashcard dal deck
     */
    public void removeFlashcard(Flashcard flashcard) {
        flashcards.remove(flashcard);
        flashcard.setDeck(null);
        totalCards = flashcards.size();
    }

    /**
     * Calcola la percentuale di completamento
     */
    public double getCompletionPercentage() {
        if (totalCards == null || totalCards == 0) return 0.0;
        return (double) cardsMastered / totalCards * 100;
    }

    /**
     * Registra una sessione di studio
     */
    public void recordStudySession() {
        if (timesStudied == null) timesStudied = 0;
        timesStudied++;
        lastStudiedAt = LocalDateTime.now();
    }

    /**
     * Aggiorna il conteggio delle carte masterizzate
     */
    public void updateMasteredCount() {
        cardsMastered = (int) flashcards.stream()
                .filter(Flashcard::isMastered)
                .count();
    }

    /**
     * Verifica se il deck necessita revisione
     */
    public boolean needsReview(int daysThreshold) {
        if (lastStudiedAt == null) return true;
        return lastStudiedAt.isBefore(LocalDateTime.now().minusDays(daysThreshold));
    }

    /**
     * Verifica se il deck è vuoto
     */
    public boolean isEmpty() {
        return totalCards == null || totalCards == 0;
    }

    /**
     * Ottiene il numero di carte attive
     */
    public long getActiveCardsCount() {
        return flashcards.stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()))
                .count();
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlashcardDeck that = (FlashcardDeck) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ==================== GETTERS & SETTERS ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<Flashcard> getFlashcards() {
        return flashcards;
    }

    public void setFlashcards(List<Flashcard> flashcards) {
        this.flashcards = flashcards;
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

    public Boolean getIsShared() {
        return isShared;
    }

    public void setIsShared(Boolean isShared) {
        this.isShared = isShared;
    }

    public Integer getTotalCards() {
        return totalCards;
    }

    public void setTotalCards(Integer totalCards) {
        this.totalCards = totalCards;
    }

    public Integer getCardsMastered() {
        return cardsMastered;
    }

    public void setCardsMastered(Integer cardsMastered) {
        this.cardsMastered = cardsMastered;
    }

    public Integer getTimesStudied() {
        return timesStudied;
    }

    public void setTimesStudied(Integer timesStudied) {
        this.timesStudied = timesStudied;
    }

    public LocalDateTime getLastStudiedAt() {
        return lastStudiedAt;
    }

    public void setLastStudiedAt(LocalDateTime lastStudiedAt) {
        this.lastStudiedAt = lastStudiedAt;
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