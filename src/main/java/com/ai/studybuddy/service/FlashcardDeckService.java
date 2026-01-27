package com.ai.studybuddy.service;

import com.ai.studybuddy.dto.FlashcardDeckCreateRequest;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.FlashcardDeckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FlashcardDeckService {

    @Autowired
    private FlashcardDeckRepository deckRepository;

    /**
     * Crea un nuovo deck
     */
    @Transactional
    public FlashcardDeck createDeck(FlashcardDeckCreateRequest request, User owner) {
        FlashcardDeck deck = new FlashcardDeck();
        deck.setName(request.getName());
        deck.setDescription(request.getDescription());
        deck.setSubject(request.getSubject());
        deck.setColor(request.getColor());
        deck.setIcon(request.getIcon());
        deck.setIsPublic(request.getIsPublic());
        deck.setOwner(owner);

        return deckRepository.save(deck);
    }

    /**
     * Ottiene tutti i deck di un utente
     */
    public List<FlashcardDeck> getUserDecks(UUID userId) {
        return deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
    }

    /**
     * Ottiene un deck specifico
     */
    public FlashcardDeck getDeck(UUID deckId, UUID userId) {
        return deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));
    }

    /**
     * Aggiorna un deck
     */
    @Transactional
    public FlashcardDeck updateDeck(UUID deckId, FlashcardDeckCreateRequest request, UUID userId) {
        FlashcardDeck deck = deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));

        deck.setName(request.getName());
        deck.setDescription(request.getDescription());
        deck.setSubject(request.getSubject());
        deck.setColor(request.getColor());
        deck.setIcon(request.getIcon());
        deck.setIsPublic(request.getIsPublic());

        return deckRepository.save(deck);
    }

    /**
     * Elimina (soft delete) un deck
     */
    @Transactional
    public void deleteDeck(UUID deckId, UUID userId) {
        FlashcardDeck deck = deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));

        deck.setIsActive(false);
        deckRepository.save(deck);
    }

    /**
     * Registra una sessione di studio
     */
    @Transactional
    public void recordStudySession(UUID deckId, UUID userId) {
        FlashcardDeck deck = deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));

        deck.recordStudySession();
        deckRepository.save(deck);
    }

    /**
     * Aggiorna il conteggio delle carte masterizzate
     */
    @Transactional
    public void updateMasteredCount(UUID deckId, UUID userId) {
        FlashcardDeck deck = deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));

        deck.updateMasteredCount();
        deckRepository.save(deck);
    }

    /**
     * Cerca deck per nome
     */
    public List<FlashcardDeck> searchDecks(UUID userId, String searchTerm) {
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

    /**
     * Classe per statistiche globali
     */
    public static class DeckGlobalStats {
        public long totalDecks;
        public long totalCards;
        public long totalMastered;
        public long totalStudySessions;

        public DeckGlobalStats(long totalDecks, long totalCards, long totalMastered, long totalStudySessions) {
            this.totalDecks = totalDecks;
            this.totalCards = totalCards;
            this.totalMastered = totalMastered;
            this.totalStudySessions = totalStudySessions;
        }
    }
}
