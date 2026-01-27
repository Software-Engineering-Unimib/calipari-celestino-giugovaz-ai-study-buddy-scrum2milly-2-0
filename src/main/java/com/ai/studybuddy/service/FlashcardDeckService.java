package com.ai.studybuddy.service;

import com.ai.studybuddy.dto.flashcard.FlashcardDeckCreateRequest;
import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.mapper.FlashcardMapper;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.FlashcardDeckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FlashcardDeckService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardDeckService.class);

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardMapper flashcardMapper;

    public FlashcardDeckService(FlashcardDeckRepository deckRepository,
                                FlashcardMapper flashcardMapper) {
        this.deckRepository = deckRepository;
        this.flashcardMapper = flashcardMapper;
    }

    /**
     * Crea un nuovo deck
     */
    @Transactional
    public FlashcardDeck createDeck(FlashcardDeckCreateRequest request, User owner) {
        log.info("Creazione deck '{}' per utente: {}", request.getName(), owner.getId());

        FlashcardDeck deck = flashcardMapper.toEntity(request, owner);
        FlashcardDeck saved = deckRepository.save(deck);

        log.info("Deck creato con ID: {}", saved.getId());
        return saved;
    }

    /**
     * Ottiene tutti i deck di un utente
     */
    public List<FlashcardDeck> getUserDecks(UUID userId) {
        log.debug("Recupero deck per utente: {}", userId);
        return deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
    }

    /**
     * Ottiene un deck specifico
     */
    public FlashcardDeck getDeck(UUID deckId, UUID userId) {
        return findDeckByIdAndOwner(deckId, userId);
    }

    /**
     * Aggiorna un deck
     */
    @Transactional
    public FlashcardDeck updateDeck(UUID deckId, FlashcardDeckCreateRequest request, UUID userId) {
        log.info("Aggiornamento deck: {}", deckId);

        FlashcardDeck deck = findDeckByIdAndOwner(deckId, userId);
        flashcardMapper.updateEntity(deck, request);

        return deckRepository.save(deck);
    }

    /**
     * Elimina (soft delete) un deck
     */
    @Transactional
    public void deleteDeck(UUID deckId, UUID userId) {
        log.info("Eliminazione deck: {}", deckId);

        FlashcardDeck deck = findDeckByIdAndOwner(deckId, userId);
        deck.setIsActive(false);
        deckRepository.save(deck);
    }

    /**
     * Registra una sessione di studio
     */
    @Transactional
    public void recordStudySession(UUID deckId, UUID userId) {
        log.debug("Registrazione sessione studio per deck: {}", deckId);

        FlashcardDeck deck = findDeckByIdAndOwner(deckId, userId);
        deck.recordStudySession();
        deckRepository.save(deck);
    }

    /**
     * Aggiorna il conteggio delle carte masterizzate
     */
    @Transactional
    public void updateMasteredCount(UUID deckId, UUID userId) {
        FlashcardDeck deck = findDeckByIdAndOwner(deckId, userId);
        deck.updateMasteredCount();
        deckRepository.save(deck);
    }

    /**
     * Cerca deck per nome
     */
    public List<FlashcardDeck> searchDecks(UUID userId, String searchTerm) {
        log.debug("Ricerca deck per utente: {}, termine: '{}'", userId, searchTerm);
        return deckRepository.searchByName(userId, searchTerm);
    }

    /**
     * Ottiene i deck pubblici (condivisi)
     */
    public List<FlashcardDeck> getPublicDecks() {
        return deckRepository.findByIsPublicTrueAndIsActiveTrueOrderByTimesStudiedDesc();
    }

    /**
     * Ottiene deck per materia
     */
    public List<FlashcardDeck> getDecksBySubject(UUID userId, String subject) {
        return deckRepository.findByOwnerIdAndSubjectAndIsActiveTrueOrderByNameAsc(userId, subject);
    }

    /**
     * Ottiene deck che necessitano revisione
     */
    public List<FlashcardDeck> getDecksNeedingReview(UUID userId, int daysAgo) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysAgo);
        return deckRepository.findNeedingReview(userId, cutoffDate);
    }

    /**
     * Ottiene statistiche globali dell'utente
     */
    public DeckGlobalStats getGlobalStats(UUID userId) {
        log.debug("Calcolo statistiche globali per utente: {}", userId);

        long totalDecks = deckRepository.countByOwnerIdAndIsActiveTrue(userId);
        long totalCards = deckRepository.countTotalCardsByOwner(userId);

        List<FlashcardDeck> decks = deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);

        long totalMastered = decks.stream()
                .mapToLong(FlashcardDeck::getCardsMastered)
                .sum();

        long totalStudySessions = decks.stream()
                .mapToLong(FlashcardDeck::getTimesStudied)
                .sum();

        return new DeckGlobalStats(totalDecks, totalCards, totalMastered, totalStudySessions);
    }

    // ==================== HELPER METHODS ====================

    private FlashcardDeck findDeckByIdAndOwner(UUID deckId, UUID userId) {
        return deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
    }

    // ==================== INNER CLASSES ====================

    /**
     * Classe per statistiche globali
     */
    public static class DeckGlobalStats {
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