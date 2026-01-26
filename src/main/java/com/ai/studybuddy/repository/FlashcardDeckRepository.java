package com.ai.studybuddy.repository;

import com.ai.studybuddy.model.FlashcardDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeck, UUID> {

    /**
     * Trova tutti i deck di un utente
     */
    List<FlashcardDeck> findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(UUID ownerId);

    /**
     * Trova deck per materia/argomento
     */
    List<FlashcardDeck> findByOwnerIdAndSubjectAndIsActiveTrueOrderByNameAsc(
        UUID ownerId, 
        String subject
    );

    /**
     * Trova deck pubblici (condivisi da altri)
     */
    List<FlashcardDeck> findByIsPublicTrueAndIsActiveTrueOrderByTimesStudiedDesc();

    /**
     * Cerca deck per nome
     */
    @Query("SELECT d FROM FlashcardDeck d WHERE d.owner.id = :ownerId " +
           "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND d.isActive = true")
    List<FlashcardDeck> searchByName(
        @Param("ownerId") UUID ownerId, 
        @Param("searchTerm") String searchTerm
    );

    /**
     * Trova i deck più studiati dell'utente
     */
    @Query("SELECT d FROM FlashcardDeck d WHERE d.owner.id = :ownerId " +
           "AND d.isActive = true " +
           "ORDER BY d.timesStudied DESC")
    List<FlashcardDeck> findMostStudied(@Param("ownerId") UUID ownerId);

    /**
     * Trova deck con maggiore completamento
     */
    @Query("SELECT d FROM FlashcardDeck d WHERE d.owner.id = :ownerId " +
           "AND d.totalCards > 0 " +
           "AND d.isActive = true " +
           "ORDER BY (CAST(d.cardsMastered AS float) / d.totalCards) DESC")
    List<FlashcardDeck> findMostCompleted(@Param("ownerId") UUID ownerId);

    /**
     * Trova deck che necessitano studio (non studiati da X giorni)
     */
    @Query("SELECT d FROM FlashcardDeck d WHERE d.owner.id = :ownerId " +
           "AND d.lastStudiedAt < :cutoffDate " +
           "AND d.isActive = true " +
           "ORDER BY d.lastStudiedAt ASC")
    List<FlashcardDeck> findNeedingReview(
        @Param("ownerId") UUID ownerId, 
        @Param("cutoffDate") java.time.LocalDateTime cutoffDate
    );

    /**
     * Conta i deck di un utente
     */
    long countByOwnerIdAndIsActiveTrue(UUID ownerId);

    /**
     * Conta le card totali di un utente (somma di tutti i deck)
     */
    @Query("SELECT COALESCE(SUM(d.totalCards), 0) FROM FlashcardDeck d " +
           "WHERE d.owner.id = :ownerId AND d.isActive = true")
    long countTotalCardsByOwner(@Param("ownerId") UUID ownerId);

    /**
     * Trova deck per ID e verifica proprietà
     */
    Optional<FlashcardDeck> findByIdAndOwnerIdAndIsActiveTrue(UUID id, UUID ownerId);
}
