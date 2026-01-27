package com.ai.studybuddy.service;

import com.ai.studybuddy.dto.flashcard.FlashcardAIGenerateRequest;
import com.ai.studybuddy.dto.flashcard.FlashcardCreateRequest;
import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.exception.UnauthorizedException;
import com.ai.studybuddy.mapper.FlashcardMapper;
import com.ai.studybuddy.model.flashcard.Flashcard;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.FlashcardRepository;
import com.ai.studybuddy.repository.FlashcardDeckRepository;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FlashcardService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardService.class);
    private static final int DEFAULT_REVIEW_DAYS = 7;

    private final FlashcardRepository flashcardRepository;
    private final FlashcardDeckRepository deckRepository;
    private final AIService aiService;
    private final FlashcardMapper flashcardMapper;

    public FlashcardService(FlashcardRepository flashcardRepository,
                            FlashcardDeckRepository deckRepository,
                            AIService aiService,
                            FlashcardMapper flashcardMapper) {
        this.flashcardRepository = flashcardRepository;
        this.deckRepository = deckRepository;
        this.aiService = aiService;
        this.flashcardMapper = flashcardMapper;
    }

    /**
     * Crea una nuova flashcard
     */
    @Transactional
    public Flashcard createFlashcard(UUID deckId, FlashcardCreateRequest request, User user) {
        log.info("Creazione flashcard nel deck: {}", deckId);

        FlashcardDeck deck = findDeckOrThrow(deckId);
        verifyOwnership(deck, user);

        Flashcard flashcard = flashcardMapper.toEntity(request, deck, user);
        Flashcard saved = flashcardRepository.save(flashcard);

        updateDeckCardCount(deck, 1);

        log.info("Flashcard creata con ID: {}", saved.getId());
        return saved;
    }

    /**
     * Genera E salva flashcard usando AI (metodo legacy per retrocompatibilità)
     * @deprecated Usa {@link #generateAndSaveFlashcards(UUID, FlashcardAIGenerateRequest, User)} invece
     */
    @Deprecated
    public List<Flashcard> generateAndSaveFlashcards(UUID deckId,
                                                     String topic,
                                                     int numberOfCards,
                                                     String difficulty,
                                                     User user) {
        FlashcardAIGenerateRequest request = FlashcardAIGenerateRequest.builder()
                .topic(topic)
                .numberOfCards(numberOfCards)
                .difficultyLevel(DifficultyLevel.fromString(difficulty))
                .build();

        return generateAndSaveFlashcards(deckId, request, user);
    }

    /**
     * Genera E salva flashcard usando AI
     */
    @Transactional
    public List<Flashcard> generateAndSaveFlashcards(UUID deckId,
                                                     FlashcardAIGenerateRequest request,
                                                     User user) {
        log.info("Generazione AI flashcards - deck: {}, topic: {}, cards: {}",
                deckId, request.getTopic(), request.getNumberOfCards());

        FlashcardDeck deck = findDeckOrThrow(deckId);
        verifyOwnership(deck, user);

        // Genera flashcard con AI
        String aiResponse = aiService.generateFlashcards(
                request.getTopic(),
                request.getNumberOfCards(),
                request.getDifficultyLevel()
        );

        // Parsa e salva
        JsonArray flashcardsJson = aiService.parseFlashcardsResponse(aiResponse);
        List<Flashcard> createdCards = new ArrayList<>();

        for (int i = 0; i < flashcardsJson.size(); i++) {
            JsonObject cardJson = flashcardsJson.get(i).getAsJsonObject();

            FlashcardCreateRequest cardRequest = FlashcardCreateRequest.builder()
                    .frontContent(cardJson.get("front").getAsString())
                    .backContent(cardJson.get("back").getAsString())
                    .difficultyLevel(request.getDifficultyLevel())
                    .tags("ai-generated", request.getTopic())
                    .build();

            Flashcard flashcard = flashcardMapper.toAIGeneratedEntity(cardRequest, deck, user);
            createdCards.add(flashcardRepository.save(flashcard));
        }

        updateDeckCardCount(deck, createdCards.size());

        log.info("Generate {} flashcards con AI", createdCards.size());
        return createdCards;
    }

    /**
     * Ottiene tutte le flashcard di un deck
     */
    public List<Flashcard> getFlashcardsByDeck(UUID deckId, UUID userId) {
        FlashcardDeck deck = findDeckOrThrow(deckId);
        verifyOwnership(deck, userId);
        return flashcardRepository.findByDeckIdAndIsActiveTrue(deckId);
    }

    /**
     * Registra una revisione di una flashcard
     */
    @Transactional
    public Flashcard reviewFlashcard(UUID flashcardId, boolean wasCorrect, UUID userId) {
        log.debug("Review flashcard: {}, correct: {}", flashcardId, wasCorrect);

        Flashcard flashcard = findFlashcardOrThrow(flashcardId);
        verifyOwnership(flashcard.getDeck(), userId);

        flashcard.recordReview(wasCorrect);
        return flashcardRepository.save(flashcard);
    }

    /**
     * Aggiorna una flashcard esistente
     */
    @Transactional
    public Flashcard updateFlashcard(UUID flashcardId, FlashcardCreateRequest request, UUID userId) {
        log.info("Aggiornamento flashcard: {}", flashcardId);

        Flashcard flashcard = findFlashcardOrThrow(flashcardId);
        verifyOwnership(flashcard.getDeck(), userId);

        flashcardMapper.updateEntity(flashcard, request);
        return flashcardRepository.save(flashcard);
    }

    /**
     * Elimina (soft delete) una flashcard
     */
    @Transactional
    public void deleteFlashcard(UUID flashcardId, UUID userId) {
        log.info("Eliminazione flashcard: {}", flashcardId);

        Flashcard flashcard = findFlashcardOrThrow(flashcardId);
        verifyOwnership(flashcard.getDeck(), userId);

        flashcard.setIsActive(false);
        flashcardRepository.save(flashcard);

        updateDeckCardCount(flashcard.getDeck(), -1);
    }

    /**
     * Ottiene flashcard casuali per una sessione di studio
     */
    public List<Flashcard> getStudySession(UUID deckId, int numberOfCards, UUID userId) {
        FlashcardDeck deck = findDeckOrThrow(deckId);
        verifyOwnership(deck, userId);

        // Priorità: 1) Mai revisionate, 2) Da ripassare, 3) Random
        List<Flashcard> neverReviewed = flashcardRepository.findNeverReviewedByDeckId(deckId);
        if (neverReviewed.size() >= numberOfCards) {
            return neverReviewed.subList(0, numberOfCards);
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(DEFAULT_REVIEW_DAYS);
        List<Flashcard> needReview = flashcardRepository.findNeedingReview(deckId, cutoffDate);
        if (needReview.size() >= numberOfCards) {
            return needReview.subList(0, numberOfCards);
        }

        return flashcardRepository.findRandomForStudy(deckId, numberOfCards);
    }

    /**
     * Cerca flashcards per contenuto
     */
    public List<Flashcard> searchFlashcards(UUID deckId, String searchTerm, UUID userId) {
        FlashcardDeck deck = findDeckOrThrow(deckId);
        verifyOwnership(deck, userId);
        return flashcardRepository.searchByContent(deckId, searchTerm);
    }

    /**
     * Ottiene statistiche delle flashcard
     */
    public FlashcardStats getFlashcardStats(UUID deckId, UUID userId) {
        FlashcardDeck deck = findDeckOrThrow(deckId);
        verifyOwnership(deck, userId);

        List<Flashcard> allCards = flashcardRepository.findByDeckIdAndIsActiveTrue(deckId);

        long total = allCards.size();
        long mastered = allCards.stream()
                .filter(card -> card.getSuccessRate() >= 80.0)
                .count();
        long needReview = allCards.stream()
                .filter(this::needsReview)
                .count();

        return new FlashcardStats(total, mastered, needReview);
    }

    // ==================== HELPER METHODS ====================

    private FlashcardDeck findDeckOrThrow(UUID deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
    }

    private Flashcard findFlashcardOrThrow(UUID flashcardId) {
        return flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard", "id", flashcardId));
    }

    private void verifyOwnership(FlashcardDeck deck, User user) {
        verifyOwnership(deck, user.getId());
    }

    private void verifyOwnership(FlashcardDeck deck, UUID userId) {
        if (!deck.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("deck", "accedere");
        }
    }

    private void updateDeckCardCount(FlashcardDeck deck, int delta) {
        deck.setTotalCards(deck.getTotalCards() + delta);
        deckRepository.save(deck);
    }

    private boolean needsReview(Flashcard card) {
        if (card.getTimesReviewed() == 0) return true;
        if (card.getLastReviewedAt() == null) return true;
        return card.getLastReviewedAt().isBefore(LocalDateTime.now().minusDays(DEFAULT_REVIEW_DAYS));
    }

    // ==================== INNER CLASSES ====================

    /**
     * Classe per le statistiche
     */
    public static class FlashcardStats {
        private final long total;
        private final long mastered;
        private final long needReview;
        private final double masteryPercentage;

        public FlashcardStats(long total, long mastered, long needReview) {
            this.total = total;
            this.mastered = mastered;
            this.needReview = needReview;
            this.masteryPercentage = total > 0 ? (double) mastered / total * 100 : 0.0;
        }

        public long getTotal() { return total; }
        public long getMastered() { return mastered; }
        public long getNeedReview() { return needReview; }
        public double getMasteryPercentage() { return masteryPercentage; }
    }
}