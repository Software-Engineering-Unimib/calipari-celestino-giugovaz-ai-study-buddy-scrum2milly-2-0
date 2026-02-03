package com.ai.studybuddy.repository;

import com.ai.studybuddy.model.user.UserProgress;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository per UserProgress
 * Traccia i progressi dell'utente per ogni argomento/topic
 */
@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

    // Trova tutti i progress di un utente
    List<UserProgress> findByUserId(UUID userId);

    // Trova progress per utente e topic specifico
    Optional<UserProgress> findByUserIdAndTopic(UUID userId, String topic);

    // Trova progress per utente e materia
    List<UserProgress> findByUserIdAndSubject(UUID userId, String subject);

    // Trova progress per utente ordinati per ultima attività
    List<UserProgress> findByUserIdOrderByLastActivityAtDesc(UUID userId);

    // Trova argomenti deboli (score < threshold)
    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId AND up.averageScore < :threshold")
    List<UserProgress> findWeakTopics(UUID userId, Double threshold);

    // Trova argomenti da ripassare (non studiati da X giorni)
    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId AND up.lastActivityAt < :cutoffDate")
    List<UserProgress> findTopicsNeedingReview(UUID userId, LocalDateTime cutoffDate);

    // Trova argomenti masterizzati
    List<UserProgress> findByUserIdAndMasteryLevel(UUID userId, DifficultyLevel masteryLevel);

    // Conta il totale degli argomenti studiati
    long countByUserId(UUID userId);

    // Conta argomenti con mastery level specifico
    long countByUserIdAndMasteryLevel(UUID userId, DifficultyLevel masteryLevel);

    // Media generale dei punteggi dell'utente
    @Query("SELECT AVG(up.averageScore) FROM UserProgress up WHERE up.user.id = :userId")
    Double getOverallAverageScore(UUID userId);

    // Totale minuti di studio
    @Query("SELECT SUM(up.totalStudyMinutes) FROM UserProgress up WHERE up.user.id = :userId")
    Integer getTotalStudyMinutes(UUID userId);

    // Totale quiz completati per utente
    @Query("SELECT SUM(up.quizCompleted) FROM UserProgress up WHERE up.user.id = :userId")
    Integer getTotalQuizCompleted(UUID userId);

    // Argomenti più studiati (per numero quiz)
    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId ORDER BY up.quizCompleted DESC")
    List<UserProgress> findMostStudiedTopics(UUID userId);

    // Argomenti recenti
    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId ORDER BY up.lastActivityAt DESC LIMIT :limit")
    List<UserProgress> findRecentTopics(UUID userId, int limit);

    // Verifica se esiste già progress per topic
    boolean existsByUserIdAndTopic(UUID userId, String topic);
}