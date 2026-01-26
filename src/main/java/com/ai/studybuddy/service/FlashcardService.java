package com.ai.studybuddy.service;

import com.ai.studybuddy.dto.FlashcardCreateRequest;
import com.ai.studybuddy.model.DifficultyLevel;
import com.ai.studybuddy.model.Flashcard;
import com.ai.studybuddy.model.FlashcardDeck;
import com.ai.studybuddy.model.User;
import com.ai.studybuddy.repository.FlashcardRepository;
import com.ai.studybuddy.repository.FlashcardDeckRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FlashcardService {

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private FlashcardDeckRepository deckRepository;

    @Autowired
    private AIService aiService;  // ← AGGIUNTO
    
    private final Gson gson = new Gson();  // ← AGGIUNTO

    /**
     * Crea una nuova flashcard
     */
    @Transactional
    public Flashcard createFlashcard(UUID deckId, FlashcardCreateRequest request, User user) {
        FlashcardDeck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));

        // Verifica che l'utente sia il proprietario del deck
        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Non autorizzato");
        }

        Flashcard flashcard = new Flashcard();
        flashcard.setFrontContent(request.getFrontContent());
        flashcard.setBackContent(request.getBackContent());
        flashcard.setHint(request.getHint());
        flashcard.setDifficultyLevel(request.getDifficultyLevel());
        flashcard.setSource(request.getSource());
        flashcard.setCreatedBy(user);
        flashcard.setDeck(deck);
        
        if (request.getTags() != null) {
            flashcard.setTagsFromArray(request.getTags());
        }

        Flashcard saved = flashcardRepository.save(flashcard);
        
        // Aggiorna il conteggio del deck
        deck.setTotalCards(deck.getTotalCards() + 1);
        deckRepository.save(deck);

        return saved;
    }

    // ==================== NUOVO METODO ==================== ←
    
    /**
     * Genera E salva flashcard usando AI
     */
    @Transactional
    public List<Flashcard> generateAndSaveFlashcards(
            UUID deckId, 
            String topic, 
            int numberOfCards, 
            String difficulty,
            User user) {
        
        // 1. Genera flashcard con AI
        String aiResponse = aiService.generateFlashCard(topic, numberOfCards, difficulty);
        
        // 2. Parsa il JSON
        JsonArray flashcardsJson = parseFlashcardsJson(aiResponse);
        
        // 3. Salva nel database
        List<Flashcard> createdCards = new ArrayList<>();
        
        for (int i = 0; i < flashcardsJson.size(); i++) {
            JsonObject cardJson = flashcardsJson.get(i).getAsJsonObject();
            
            FlashcardCreateRequest request = new FlashcardCreateRequest();
            request.setFrontContent(cardJson.get("front").getAsString());
            request.setBackContent(cardJson.get("back").getAsString());
            request.setDifficultyLevel(DifficultyLevel.valueOf(difficulty.toUpperCase()));
            request.setTags(new String[]{"ai-generated", topic});
            
            Flashcard created = createFlashcard(deckId, request, user);
            createdCards.add(created);
        }
        
        return createdCards;
    }
    
    /**
     * Helper per parsing JSON da AI
     */
    private JsonArray parseFlashcardsJson(String aiResponse) {
        String cleaned = aiResponse
            .replaceAll("```json\\s*", "")
            .replaceAll("```\\s*", "")
            .trim();
        return gson.fromJson(cleaned, JsonArray.class);
    }

    // ==================== METODI ESISTENTI (invariati) ====================

    /**
     * Ottiene tutte le flashcard di un deck
     */
    public List<Flashcard> getFlashcardsByDeck(UUID deckId, UUID userId) {
        // Verifica proprietà del deck
        FlashcardDeck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));

        if (!deck.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        return flashcardRepository.findByDeckIdAndIsActiveTrue(deckId);
    }

    /**
     * Registra una revisione di una flashcard
     */
    @Transactional
    public Flashcard reviewFlashcard(UUID flashcardId, boolean wasCorrect, UUID userId) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
            .orElseThrow(() -> new RuntimeException("Flashcard non trovata"));

        // Verifica proprietà
        if (!flashcard.getDeck().getOwner().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        flashcard.recordReview(wasCorrect);
        
        return flashcardRepository.save(flashcard);
    }

    /**
     * Aggiorna una flashcard esistente
     */
    @Transactional
    public Flashcard updateFlashcard(UUID flashcardId, FlashcardCreateRequest request, UUID userId) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
            .orElseThrow(() -> new RuntimeException("Flashcard non trovata"));

        if (!flashcard.getDeck().getOwner().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        flashcard.setFrontContent(request.getFrontContent());
        flashcard.setBackContent(request.getBackContent());
        flashcard.setHint(request.getHint());
        flashcard.setDifficultyLevel(request.getDifficultyLevel());
        flashcard.setSource(request.getSource());
        
        if (request.getTags() != null) {
            flashcard.setTagsFromArray(request.getTags());
        }

        return flashcardRepository.save(flashcard);
    }

    /**
     * Elimina (soft delete) una flashcard
     */
    @Transactional
    public void deleteFlashcard(UUID flashcardId, UUID userId) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
            .orElseThrow(() -> new RuntimeException("Flashcard non trovata"));

        if (!flashcard.getDeck().getOwner().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        flashcard.setIsActive(false);
        flashcardRepository.save(flashcard);

        // Aggiorna il conteggio del deck
        FlashcardDeck deck = flashcard.getDeck();
        deck.setTotalCards(deck.getTotalCards() - 1);
        deckRepository.save(deck);
    }

    /**
     * Ottiene flashcard casuali per una sessione di studio
     */
    public List<Flashcard> getStudySession(UUID deckId, int numberOfCards, UUID userId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));

        if (!deck.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        // Priorità: 1) Mai revisionate, 2) Difficili, 3) Random
        List<Flashcard> neverReviewed = flashcardRepository.findNeverReviewedByDeckId(deckId);
        
        if (neverReviewed.size() >= numberOfCards) {
            return neverReviewed.subList(0, numberOfCards);
        }

        List<Flashcard> needReview = flashcardRepository.findNeedingReview(
            deckId, 
            LocalDateTime.now().minusDays(7)
        );

        if (needReview.size() >= numberOfCards) {
            return needReview.subList(0, numberOfCards);
        }

        return flashcardRepository.findRandomForStudy(deckId, numberOfCards);
    }

    /**
     * Cerca flashcards per contenuto
     */
    public List<Flashcard> searchFlashcards(UUID deckId, String searchTerm, UUID userId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));

        if (!deck.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        return flashcardRepository.searchByContent(deckId, searchTerm);
    }

    /**
     * Ottiene statistiche delle flashcard
     */
    public FlashcardStats getFlashcardStats(UUID deckId, UUID userId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new RuntimeException("Deck non trovato"));

        if (!deck.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        List<Flashcard> allCards = flashcardRepository.findByDeckIdAndIsActiveTrue(deckId);
        
        long total = allCards.size();
        long mastered = allCards.stream()
            .filter(card -> card.getSuccessRate() >= 80.0)
            .count();
        long needReview = allCards.stream()
            .filter(card -> card.getTimesReviewed() == 0 || 
                           (card.getLastReviewedAt() != null && 
                            card.getLastReviewedAt().isBefore(LocalDateTime.now().minusDays(7))))
            .count();

        return new FlashcardStats(total, mastered, needReview);
    }

    /**
     * Classe per le statistiche
     */
    public static class FlashcardStats {
        public long total;
        public long mastered;
        public long needReview;

        public FlashcardStats(long total, long mastered, long needReview) {
            this.total = total;
            this.mastered = mastered;
            this.needReview = needReview;
        }
    }
}