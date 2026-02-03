package com.ai.studybuddy.model.gamification;

import com.ai.studybuddy.model.user.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entità UserStats - statistiche GLOBALI dell'utente per gamification
 *
 * NOTA: Questa classe si integra con UserProgress che traccia i progressi per argomento.
 * - UserProgress → statistiche PER TOPIC (quiz, score, mastery per argomento)
 * - UserStats → statistiche GLOBALI (XP totali, livello, streak, badge)
 */
@Entity
@Table(name = "user_stats")
public class UserStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ==================== PUNTI XP ====================

    @Column(name = "total_xp")
    private Integer totalXp = 0;

    @Column(name = "weekly_xp")
    private Integer weeklyXp = 0;

    @Column(name = "monthly_xp")
    private Integer monthlyXp = 0;

    // ==================== CONTATORI ATTIVITÀ ====================

    @Column(name = "explanations_requested")
    private Integer explanationsRequested = 0;

    @Column(name = "quizzes_completed")
    private Integer quizzesCompleted = 0;

    @Column(name = "quizzes_passed")
    private Integer quizzesPassed = 0;

    @Column(name = "flashcards_studied")
    private Integer flashcardsStudied = 0;

    @Column(name = "flashcards_mastered")
    private Integer flashcardsMastered = 0;

    @Column(name = "focus_sessions_completed")
    private Integer focusSessionsCompleted = 0;

    @Column(name = "total_study_time_minutes")
    private Integer totalStudyTimeMinutes = 0;

    // ==================== STREAK ====================

    @Column(name = "current_streak")
    private Integer currentStreak = 0;

    @Column(name = "longest_streak")
    private Integer longestStreak = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    // ==================== LIVELLO ====================

    @Column(name = "level")
    private Integer level = 1;

    @Column(name = "xp_for_next_level")
    private Integer xpForNextLevel = 100;

    // ==================== AUDIT ====================

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
     * Aggiunge XP e aggiorna il livello se necessario
     * @return true se l'utente è salito di livello
     */
    public boolean addXp(int xp) {
        this.totalXp += xp;
        this.weeklyXp += xp;
        this.monthlyXp += xp;

        // Controlla level up
        boolean leveledUp = false;
        while (this.totalXp >= this.xpForNextLevel) {
            this.level++;
            this.xpForNextLevel = calculateXpForLevel(this.level + 1);
            leveledUp = true;
        }

        return leveledUp;
    }

    /**
     * Calcola XP necessari per un livello
     * Formula: 100 * livello^1.5
     */
    private int calculateXpForLevel(int level) {
        return (int) (100 * Math.pow(level, 1.5));
    }

    /**
     * Aggiorna lo streak giornaliero
     */
    public void updateStreak() {
        LocalDate today = LocalDate.now();

        if (lastActivityDate == null) {
            currentStreak = 1;
        } else if (lastActivityDate.equals(today.minusDays(1))) {
            currentStreak++;
        } else if (!lastActivityDate.equals(today)) {
            currentStreak = 1;
        }
        // Se è lo stesso giorno, non cambia nulla

        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }

        lastActivityDate = today;
    }

    /**
     * Calcola la percentuale di progresso verso il prossimo livello
     */
    public double getLevelProgressPercentage() {
        int xpForCurrentLevel = level > 1 ? calculateXpForLevel(level) : 0;
        int xpInCurrentLevel = totalXp - xpForCurrentLevel;
        int xpNeededForLevel = xpForNextLevel - xpForCurrentLevel;

        if (xpNeededForLevel <= 0) return 100.0;
        return (double) xpInCurrentLevel / xpNeededForLevel * 100;
    }

    /**
     * Incrementa contatore spiegazioni
     */
    public void incrementExplanations() {
        this.explanationsRequested++;
    }

    /**
     * Incrementa contatore quiz completati
     */
    public void incrementQuizzesCompleted(boolean passed) {
        this.quizzesCompleted++;
        if (passed) {
            this.quizzesPassed++;
        }
    }

    /**
     * Incrementa contatore flashcards studiate
     */
    public void incrementFlashcardsStudied(int count) {
        this.flashcardsStudied += count;
    }

    /**
     * Incrementa contatore sessioni focus
     */
    public void incrementFocusSessions(int durationMinutes) {
        this.focusSessionsCompleted++;
        this.totalStudyTimeMinutes += durationMinutes;
    }

    // ==================== GETTERS & SETTERS ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(Integer totalXp) {
        this.totalXp = totalXp;
    }

    public Integer getWeeklyXp() {
        return weeklyXp;
    }

    public void setWeeklyXp(Integer weeklyXp) {
        this.weeklyXp = weeklyXp;
    }

    public Integer getMonthlyXp() {
        return monthlyXp;
    }

    public void setMonthlyXp(Integer monthlyXp) {
        this.monthlyXp = monthlyXp;
    }

    public Integer getExplanationsRequested() {
        return explanationsRequested;
    }

    public void setExplanationsRequested(Integer explanationsRequested) {
        this.explanationsRequested = explanationsRequested;
    }

    public Integer getQuizzesCompleted() {
        return quizzesCompleted;
    }

    public void setQuizzesCompleted(Integer quizzesCompleted) {
        this.quizzesCompleted = quizzesCompleted;
    }

    public Integer getQuizzesPassed() {
        return quizzesPassed;
    }

    public void setQuizzesPassed(Integer quizzesPassed) {
        this.quizzesPassed = quizzesPassed;
    }

    public Integer getFlashcardsStudied() {
        return flashcardsStudied;
    }

    public void setFlashcardsStudied(Integer flashcardsStudied) {
        this.flashcardsStudied = flashcardsStudied;
    }

    public Integer getFlashcardsMastered() {
        return flashcardsMastered;
    }

    public void setFlashcardsMastered(Integer flashcardsMastered) {
        this.flashcardsMastered = flashcardsMastered;
    }

    public Integer getFocusSessionsCompleted() {
        return focusSessionsCompleted;
    }

    public void setFocusSessionsCompleted(Integer focusSessionsCompleted) {
        this.focusSessionsCompleted = focusSessionsCompleted;
    }

    public Integer getTotalStudyTimeMinutes() {
        return totalStudyTimeMinutes;
    }

    public void setTotalStudyTimeMinutes(Integer totalStudyTimeMinutes) {
        this.totalStudyTimeMinutes = totalStudyTimeMinutes;
    }

    public Integer getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }

    public Integer getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(Integer longestStreak) {
        this.longestStreak = longestStreak;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getXpForNextLevel() {
        return xpForNextLevel;
    }

    public void setXpForNextLevel(Integer xpForNextLevel) {
        this.xpForNextLevel = xpForNextLevel;
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