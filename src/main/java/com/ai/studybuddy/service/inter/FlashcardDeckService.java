package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.dto.flashcard.FlashcardDeckCreateRequest;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;

import java.util.List;
import java.util.UUID;

/**
 * Interfaccia per il servizio FlashcardDeck
 */
public interface FlashcardDeckService {

    /**
     * Crea un nuovo deck
     */
    FlashcardDeck createDeck(FlashcardDeckCreateRequest request, User owner);

    /**
     * Ottiene tutti i deck di un utente
     */
    List<FlashcardDeck> getUserDecks(UUID userId);

    /**
     * Ottiene un deck specifico
     */
    FlashcardDeck getDeck(UUID deckId, UUID userId);

    /**
     * Aggiorna un deck
     */
    FlashcardDeck updateDeck(UUID deckId, FlashcardDeckCreateRequest request, UUID userId);

    /**
     * Elimina (soft delete) un deck
     */
    void deleteDeck(UUID deckId, UUID userId);

    /**
     * Registra una sessione di studio
     */
    void recordStudySession(UUID deckId, UUID userId);

    /**
     * Aggiorna il conteggio delle carte masterizzate
     */
    void updateMasteredCount(UUID deckId, UUID userId);

    /**
     * Cerca deck per nome
     */
    List<FlashcardDeck> searchDecks(UUID userId, String searchTerm);

    /**
     * Ottiene i deck pubblici (condivisi)
     */
    List<FlashcardDeck> getPublicDecks();

    /**
     * Ottiene deck per materia
     */
    List<FlashcardDeck> getDecksBySubject(UUID userId, String subject);

    /**
     * Ottiene deck che necessitano revisione
     */
    List<FlashcardDeck> getDecksNeedingReview(UUID userId, int daysAgo);

    /**
     * Ottiene statistiche globali dell'utente
     */
    DeckGlobalStats getGlobalStats(UUID userId);

    /**
     * Classe per statistiche globali
     */
    class DeckGlobalStats {
        private final long totalDecks;
        private final long totalCards;
        private final long totalMastered;
        private final long totalStudySessions;
        private final double overallMasteryPercentage;

        public DeckGlobalStats(long totalDecks, long totalCards,
                               long totalMastered, long totalStudySessions) {
            this.totalDecks = totalDecks;
            this.totalCards = totalCards;
            this.totalMastered = totalMastered;
            this.totalStudySessions = totalStudySessions;
            this.overallMasteryPercentage = totalCards > 0
                    ? (double) totalMastered / totalCards * 100 : 0.0;
        }

        public long getTotalDecks() { return totalDecks; }
        public long getTotalCards() { return totalCards; }
        public long getTotalMastered() { return totalMastered; }
        public long getTotalStudySessions() { return totalStudySessions; }
        public double getOverallMasteryPercentage() { return overallMasteryPercentage; }
    }
}
