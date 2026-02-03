package com.ai.studybuddy.repository;

import com.ai.studybuddy.model.gamification.Recommendation;
import com.ai.studybuddy.model.gamification.Recommendation.RecommendationType;
import com.ai.studybuddy.model.gamification.Recommendation.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    // Raccomandazioni attive per utente
    @Query("SELECT r FROM Recommendation r WHERE r.user.id = :userId " +
            "AND r.isDismissed = false AND r.isCompleted = false " +
            "AND (r.expiresAt IS NULL OR r.expiresAt > :now) " +
            "ORDER BY r.priority DESC, r.createdAt DESC")
    List<Recommendation> findActiveByUserId(UUID userId, LocalDateTime now);

    // Raccomandazioni per tipo
    List<Recommendation> findByUserIdAndTypeAndIsDismissedFalseAndIsCompletedFalse(
            UUID userId, RecommendationType type);

    // Raccomandazioni ad alta priorità
    @Query("SELECT r FROM Recommendation r WHERE r.user.id = :userId " +
            "AND r.isDismissed = false AND r.isCompleted = false " +
            "AND r.priority IN ('HIGH', 'URGENT') " +
            "ORDER BY r.priority DESC, r.createdAt DESC")
    List<Recommendation> findHighPriorityByUserId(UUID userId);

    // Conta raccomandazioni attive
    @Query("SELECT COUNT(r) FROM Recommendation r WHERE r.user.id = :userId " +
            "AND r.isDismissed = false AND r.isCompleted = false " +
            "AND (r.expiresAt IS NULL OR r.expiresAt > :now)")
    long countActiveByUserId(UUID userId, LocalDateTime now);

    // Pulisci raccomandazioni scadute
    @Modifying
    @Query("DELETE FROM Recommendation r WHERE r.expiresAt < :now")
    void deleteExpired(LocalDateTime now);

    // Pulisci vecchie raccomandazioni completate/ignorate
    @Modifying
    @Query("DELETE FROM Recommendation r WHERE " +
            "(r.isDismissed = true OR r.isCompleted = true) " +
            "AND r.createdAt < :cutoff")
    void deleteOldCompleted(LocalDateTime cutoff);

    // Verifica se esiste già una raccomandazione simile
    boolean existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
            UUID userId, RecommendationType type, String topic);

    // Raccomandazioni per argomento
    List<Recommendation> findByUserIdAndTopicContainingIgnoreCaseAndIsDismissedFalse(
            UUID userId, String topic);
}