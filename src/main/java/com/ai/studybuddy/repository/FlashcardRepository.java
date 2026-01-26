package com.ai.studybuddy.repository;

import com.ai.studybuddy.model.Flashcard;
import com.ai.studybuddy.model.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {

    /**
     * Trova tutte le flashcard di un deck specifico
     */
    List<Flashcard> findByDeckIdAndIsActiveTrue(UUID deckId);

    /**
     * Trova flashcard per livello di difficoltà
     */
    List<Flashcard> findByDeckIdAndDifficultyLevelAndIsActiveTrue(
        UUID deckId, 
        DifficultyLevel difficultyLevel
    );

    /**
     * Trova flashcard generate dall'AI
     */
    List<Flashcard> findByDeckIdAndAiGeneratedTrueAndIsActiveTrue(UUID deckId);

    /**
     * Trova flashcard mai revisionate
     */
    @Query("SELECT f FROM Flashcard f WHERE f.deck.id = :deckId " +
           "AND f.timesReviewed = 0 AND f.isActive = true")
    List<Flashcard> findNeverReviewedByDeckId(@Param("deckId") UUID deckId);

    /**
     * Trova flashcard che necessitano revisione (non revisionate da X giorni)
     */
    @Query("SELECT f FROM Flashcard f WHERE f.deck.id = :deckId " +
           "AND (f.lastReviewedAt IS NULL OR f.lastReviewedAt < :cutoffDate) " +
           "AND f.isActive = true " +
           "ORDER BY f.lastReviewedAt ASC NULLS FIRST")
    List<Flashcard> findNeedingReview(
        @Param("deckId") UUID deckId, 
        @Param("cutoffDate") LocalDateTime cutoffDate
    );

    /**
     * Trova le flashcard più difficili (bassa percentuale successo)
     */
    @Query("SELECT f FROM Flashcard f WHERE f.deck.id = :deckId " +
           "AND f.timesReviewed >= 3 " +
           "AND f.isActive = true " +
           "ORDER BY (CAST(f.timesCorrect AS float) / f.timesReviewed) ASC")
    List<Flashcard> findMostDifficult(@Param("deckId") UUID deckId);

    /**
     * Cerca flashcard per contenuto (full-text search)
     */
    @Query("SELECT f FROM Flashcard f WHERE f.deck.id = :deckId " +
           "AND (LOWER(f.frontContent) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(f.backContent) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND f.isActive = true")
    List<Flashcard> searchByContent(
        @Param("deckId") UUID deckId, 
        @Param("searchTerm") String searchTerm
    );

    /**
     * Trova flashcard per tag
     */
    @Query("SELECT f FROM Flashcard f WHERE f.deck.id = :deckId " +
           "AND LOWER(f.tags) LIKE LOWER(CONCAT('%', :tag, '%')) " +
           "AND f.isActive = true")
    List<Flashcard> findByTag(
        @Param("deckId") UUID deckId, 
        @Param("tag") String tag
    );

    /**
     * Conta le flashcard per deck
     */
    long countByDeckIdAndIsActiveTrue(UUID deckId);

    /**
     * Conta le flashcard masterizzate (>80% successo)
     */
    @Query("SELECT COUNT(f) FROM Flashcard f WHERE f.deck.id = :deckId " +
           "AND f.timesReviewed > 0 " +
           "AND (CAST(f.timesCorrect AS float) / f.timesReviewed) >= 0.8 " +
           "AND f.isActive = true")
    long countMasteredByDeckId(@Param("deckId") UUID deckId);

    /**
     * Trova flashcard random per sessione di studio
     */
    @Query(value = "SELECT * FROM flashcards f WHERE f.deck_id = :deckId " +
                   "AND f.is_active = true ORDER BY RANDOM() LIMIT :limit", 
           nativeQuery = true)
    List<Flashcard> findRandomForStudy(
        @Param("deckId") UUID deckId, 
        @Param("limit") int limit
    );
}
