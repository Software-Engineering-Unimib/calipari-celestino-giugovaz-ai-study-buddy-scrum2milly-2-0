package com.ai.studybuddy.model.quiz;

import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity Quiz - rappresenta un quiz con domande a scelta multipla
 */
@Entity
@Table(name = "quizzes", indexes = {
        @Index(name = "idx_quiz_user", columnList = "user_id"),
        @Index(name = "idx_quiz_completed", columnList = "is_completed"),
        @Index(name = "idx_quiz_subject", columnList = "subject")
})
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ==================== RELAZIONI ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<Question> questions = new ArrayList<>();

    // ==================== DATI QUIZ ====================

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "subject", length = 100)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level")
    private DifficultyLevel difficultyLevel;

    @Column(name = "number_of_questions")
    private Integer numberOfQuestions;

    // ==================== STATO ====================

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "is_ai_generated")
    private Boolean isAiGenerated = false;

    // ==================== RISULTATI ====================

    @Column(name = "score")
    private Integer score;  // Risposte corrette

    @Column(name = "total_points")
    private Integer totalPoints;  // Punti totali possibili

    @Column(name = "percentage")
    private Double percentage;  // Percentuale di successo

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;  // Tempo impiegato

    // ==================== TIMESTAMP ====================

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isCompleted == null) isCompleted = false;
        if (isAiGenerated == null) isAiGenerated = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * Aggiunge una domanda al quiz
     */
    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuiz(this);
        question.setQuestionOrder(questions.size());
        numberOfQuestions = questions.size();
    }

    /**
     * Rimuove una domanda dal quiz
     */
    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setQuiz(null);
        numberOfQuestions = questions.size();
        // Riordina le domande rimanenti
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setQuestionOrder(i + 1);
        }
    }

    /**
     * Inizia il quiz
     */
    public void start() {
        this.startedAt = LocalDateTime.now();
        this.isCompleted = false;
    }

    /**
     * Completa il quiz e calcola il punteggio
     */
    public void complete() {
        this.completedAt = LocalDateTime.now();
        this.isCompleted = true;
        calculateScore();

        if (startedAt != null) {
            this.timeSpentSeconds = (int) java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
    }

    /**
     * Calcola il punteggio
     */
    public void calculateScore() {
        if (questions == null || questions.isEmpty()) {
            this.score = 0;
            this.totalPoints = 0;
            this.percentage = 0.0;
            return;
        }

        this.totalPoints = questions.size();
        this.score = (int) questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsCorrect()))
                .count();
        this.percentage = (double) score / totalPoints * 100;
    }

    /**
     * Verifica se tutte le domande sono state risposte
     */
    public boolean allQuestionsAnswered() {
        return questions.stream().allMatch(Question::isAnswered);
    }

    /**
     * Conta le domande risposte
     */
    public long getAnsweredCount() {
        return questions.stream().filter(Question::isAnswered).count();
    }

    /**
     * Conta le domande corrette
     */
    public long getCorrectCount() {
        return questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsCorrect()))
                .count();
    }

    /**
     * Resetta tutte le risposte
     */
    public void resetAllAnswers() {
        questions.forEach(Question::resetAnswer);
        this.isCompleted = false;
        this.score = null;
        this.percentage = null;
        this.startedAt = null;
        this.completedAt = null;
        this.timeSpentSeconds = null;
    }

    /**
     * Verifica se il quiz è stato superato (>=60%)
     */
    public boolean isPassed() {
        return percentage != null && percentage >= 60.0;
    }

    /**
     * Ottiene il tempo formattato (mm:ss)
     */
    public String getFormattedTime() {
        if (timeSpentSeconds == null) return "00:00";
        int minutes = timeSpentSeconds / 60;
        int seconds = timeSpentSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quiz quiz = (Quiz) o;
        return Objects.equals(id, quiz.id);
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public Boolean getIsAiGenerated() {
        return isAiGenerated;
    }

    public void setIsAiGenerated(Boolean isAiGenerated) {
        this.isAiGenerated = isAiGenerated;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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