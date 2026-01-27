package com.ai.studybuddy.repository;

import com.ai.studybuddy.model.quiz.Quiz;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    // Trova quiz per utente
    List<Quiz> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Trova quiz completati per utente
    List<Quiz> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(UUID userId);

    // Trova quiz non completati per utente
    List<Quiz> findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(UUID userId);

    // Trova quiz per topic
    List<Quiz> findByUserIdAndTopicContainingIgnoreCaseOrderByCreatedAtDesc(UUID userId, String topic);

    // Trova quiz per materia
    List<Quiz> findByUserIdAndSubjectOrderByCreatedAtDesc(UUID userId, String subject);

    // Trova quiz per difficoltà
    List<Quiz> findByUserIdAndDifficultyLevelOrderByCreatedAtDesc(UUID userId, DifficultyLevel difficultyLevel);

    // Trova quiz con ID e utente
    Optional<Quiz> findByIdAndUserId(UUID quizId, UUID userId);

    // Conta quiz completati per utente
    long countByUserIdAndIsCompletedTrue(UUID userId);

    // Conta quiz totali per utente
    long countByUserId(UUID userId);

    // Media percentuale quiz completati
    @Query("SELECT AVG(q.percentage) FROM Quiz q WHERE q.user.id = :userId AND q.isCompleted = true")
    Double getAverageScoreByUserId(@Param("userId") UUID userId);

    // Quiz recenti (ultimi N giorni)
    @Query("SELECT q FROM Quiz q WHERE q.user.id = :userId AND q.createdAt >= :since ORDER BY q.createdAt DESC")
    List<Quiz> findRecentQuizzes(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    // Quiz superati
    @Query("SELECT q FROM Quiz q WHERE q.user.id = :userId AND q.isCompleted = true AND q.percentage >= 60.0 ORDER BY q.completedAt DESC")
    List<Quiz> findPassedQuizzes(@Param("userId") UUID userId);

    // Quiz falliti
    @Query("SELECT q FROM Quiz q WHERE q.user.id = :userId AND q.isCompleted = true AND q.percentage < 60.0 ORDER BY q.completedAt DESC")
    List<Quiz> findFailedQuizzes(@Param("userId") UUID userId);

    // Miglior punteggio per topic
    @Query("SELECT MAX(q.percentage) FROM Quiz q WHERE q.user.id = :userId AND q.topic = :topic AND q.isCompleted = true")
    Double getBestScoreByTopic(@Param("userId") UUID userId, @Param("topic") String topic);
}