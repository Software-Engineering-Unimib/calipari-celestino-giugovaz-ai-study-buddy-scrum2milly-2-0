package com.ai.studybuddy.model.user;

import com.ai.studybuddy.util.enums.DifficultyLevel;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_progress")
public class UserProgress {



        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @ManyToOne
        @JoinColumn(name = "user_id")
        private User user;

        // Argomento specifico (es: "Teorema di Pitagora", "Derivate")
        private String topic;

        // Materia generale (es: "Matematica", "Fisica")
        private String subject;

        // Statistiche
        private Integer quizCompleted = 0;
        private Double averageScore = 0.0;        // Media punteggi (0-100)
        private Integer totalQuestions = 0;
        private Integer correctAnswers = 0;

        // Livello calcolato automaticamente
        @Enumerated(EnumType.STRING)
            private DifficultyLevel masteryLevel = DifficultyLevel.PRINCIPIANTE;

        private Integer totalStudyMinutes = 0;

        // Ultima attività su questo argomento
        private LocalDateTime lastActivityAt;
    }
