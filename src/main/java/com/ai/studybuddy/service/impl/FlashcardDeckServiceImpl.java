package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.flashcard.FlashcardDeckCreateRequest;
import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.mapper.FlashcardMapper;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.FlashcardDeckRepository;
import com.ai.studybuddy.service.inter.FlashcardDeckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FlashcardDeckServiceImpl implements FlashcardDeckService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardDeckServiceImpl.class);

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardMapper flashcardMapper;

    public FlashcardDeckServiceImpl(FlashcardDeckRepository deckRepository,
                                    FlashcardMapper flashcardMapper) {
        this.deckRepository = deckRepository;
        this.flashcardMapper = flashcardMapper;
    }

    @Override
    @Transactional
    public FlashcardDeck createDeck(FlashcardDeckCreateRequest request, User owner) {
        log.info("Creazione deck '{}' per utente: {}", request.getName(), owner.getId());

        FlashcardDeck deck = flashcardMapper.toEntity(request, owner);
        FlashcardDeck saved = deckRepository.save(deck);

        log.info("Deck creato con ID: {}", saved.getId());
        return saved;
    }

    @Override
    public List<FlashcardDeck> getUserDecks(UUID userId) {
        log.debug("Recupero deck per utente: {}", userId);
        return deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
    }

    @Override
    public FlashcardDeck getDeck(UUID deckId, UUID userId) {
        return findDeckByIdAndOwner(deckId, userId);
    }

    @Override
    @Transactional
    public FlashcardDeck updateDeck(UUID deckId, FlashcardDeckCreateRequest request, UUID userId) {
        log.info("Aggiornamento deck: {}", deckId);

        FlashcardDeck deck = findDeckByIdAndOwner(deckId, userId);
        flashcardMapper.updateEntity(deck, request);

        return deckRepository.save(deck);
    }

    @Override
    @Transactional
    public void deleteDeck(UUID deckId, UUID userId) {
        log.info("Eliminazione deck: {}", deckId);

        FlashcardDeck deck = findDeckByIdAndOwner(deckId, userId);
        deck.setIsActive(false);
        deckRepository.save(deck);
    }

    @Override
    @Transactional
    public void recordStudySession(UUID deckId, UUID userId) {
        log.debug("Registrazione sessione studio per deck: {}", deckId);

        FlashcardDeck deck = findDeckByIdAndOwner(deckId, userId);
        deck.recordStudySession();
        deckRepository.save(deck);
    }

    @Override
    @Transactional
    public void updateMasteredCount(UUID deckId, UUID userId) {
        FlashcardDeck deck = findDeckByIdAndOwner(deckId, userId);
        deck.updateMasteredCount();
        deckRepository.save(deck);
    }

    @Override
    public List<FlashcardDeck> searchDecks(UUID userId, String searchTerm) {
        log.debug("Ricerca deck per utente: {}, termine: '{}'", userId, searchTerm);
        return deckRepository.searchByName(userId, searchTerm);
    }

    @Override
    public List<FlashcardDeck> getPublicDecks() {
        return deckRepository.findByIsPublicTrueAndIsActiveTrueOrderByTimesStudiedDesc();
    }

    @Override
    public List<FlashcardDeck> getDecksBySubject(UUID userId, String subject) {
        return deckRepository.findByOwnerIdAndSubjectAndIsActiveTrueOrderByNameAsc(userId, subject);
    }

    @Override
    public List<FlashcardDeck> getDecksNeedingReview(UUID userId, int daysAgo) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysAgo);
        return deckRepository.findNeedingReview(userId, cutoffDate);
    }

    @Override
    public FlashcardDeckService.DeckGlobalStats getGlobalStats(UUID userId) {
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

        return new FlashcardDeckService.DeckGlobalStats(totalDecks, totalCards, totalMastered, totalStudySessions);
    }

    // ==================== HELPER METHODS ====================

    private FlashcardDeck findDeckByIdAndOwner(UUID deckId, UUID userId) {
        return deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
    }
}