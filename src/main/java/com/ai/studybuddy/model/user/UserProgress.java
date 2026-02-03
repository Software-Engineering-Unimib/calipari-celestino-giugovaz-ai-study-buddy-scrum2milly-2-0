package com.ai.studybuddy.model.user;

import com.ai.studybuddy.util.enums.DifficultyLevel;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "topic"}))
public class UserProgress {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        // Argomento specifico (es: "Teorema di Pitagora")
        @Column(nullable = false)
        private String topic;

        // Materia generale (es: "Matematica")
        private String subject;

        // ==================== STATISTICHE ====================

        private Integer quizCompleted = 0;
        private Double averageScore = 0.0;        // Media % (0–100)
        private Integer totalQuestions = 0;
        private Integer correctAnswers = 0;

        @Enumerated(EnumType.STRING)
        private DifficultyLevel masteryLevel = DifficultyLevel.PRINCIPIANTE;

        private Integer totalStudyMinutes = 0;

        private LocalDateTime lastActivityAt;

        // ==================== BUSINESS METHODS ====================

        /** Incrementa quiz completati */
        public void incrementQuizCompleted(int value) {
                this.quizCompleted = safe(this.quizCompleted) + value;
        }

        /** Aggiunge risultati di un quiz e ricalcola media */
        public void addQuizResult(int totalQ, int correctQ) {
                this.totalQuestions = safe(this.totalQuestions) + totalQ;
                this.correctAnswers = safe(this.correctAnswers) + correctQ;
                recalculateAverage();
        }

        /** Ricalcola la media punteggi */
        public void recalculateAverage() {
                if (safe(this.totalQuestions) > 0) {
                        this.averageScore = (this.correctAnswers * 100.0) / this.totalQuestions;
                } else {
                        this.averageScore = 0.0;
                }
        }

        /** Aggiorna livello di padronanza */
        public void updateMasteryLevel() {
                if (averageScore == null) {
                        this.masteryLevel = DifficultyLevel.PRINCIPIANTE;
                } else if (averageScore >= 90) {
                        this.masteryLevel = DifficultyLevel.AVANZATO;
                } else if (averageScore >= 70) {
                        this.masteryLevel = DifficultyLevel.INTERMEDIO;
                } else {
                        this.masteryLevel = DifficultyLevel.PRINCIPIANTE;
                }
        }

        /** Aggiunge minuti di studio */
        public void addStudyMinutes(int minutes) {
                this.totalStudyMinutes = safe(this.totalStudyMinutes) + minutes;
        }

        /** Aggiorna ultima attività */
        public void touch() {
                this.lastActivityAt = LocalDateTime.now();
        }

        private int safe(Integer value) {
                return value == null ? 0 : value;
        }

        // ==================== GETTER & SETTER ====================

        public UUID getId() { return id; }

        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public Integer getQuizCompleted() { return safe(quizCompleted); }
        public void setQuizCompleted(Integer quizCompleted) { this.quizCompleted = quizCompleted; }

        public Double getAverageScore() { return averageScore == null ? 0.0 : averageScore; }
        public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }

        public Integer getTotalQuestions() { return safe(totalQuestions); }
        public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

        public Integer getCorrectAnswers() { return safe(correctAnswers); }
        public void setCorrectAnswers(Integer correctAnswers) { this.correctAnswers = correctAnswers; }

        public DifficultyLevel getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(DifficultyLevel masteryLevel) { this.masteryLevel = masteryLevel; }

        public Integer getTotalStudyMinutes() { return safe(totalStudyMinutes); }
        public void setTotalStudyMinutes(Integer totalStudyMinutes) { this.totalStudyMinutes = totalStudyMinutes; }

        public LocalDateTime getLastActivityAt() { return lastActivityAt; }
        public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
}