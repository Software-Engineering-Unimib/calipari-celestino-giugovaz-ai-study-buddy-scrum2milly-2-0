package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.FlashcardCreateRequest;
import com.ai.studybuddy.dto.FlashcardDeckCreateRequest;
import com.ai.studybuddy.model.Flashcard;
import com.ai.studybuddy.model.FlashcardDeck;
import com.ai.studybuddy.model.User;
import com.ai.studybuddy.repository.UserRepository;
import com.ai.studybuddy.service.FlashcardDeckService;
import com.ai.studybuddy.service.FlashcardService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {

    @Autowired
    private FlashcardService flashcardService;

    @Autowired
    private FlashcardDeckService deckService;
    
    @Autowired
    private UserRepository userRepository;
    private User getCurrentUser() {
        return userRepository.findByEmail("demo@studybuddy.com")
            .orElseGet(() -> {
                User user = new User();
                // NON impostare ID - auto-generato
                user.setFirstName("Demo");
                user.setLastName("User");
                user.setEmail("demo@studybuddy.com");
                user.setPasswordHash("demo-hash");
                return userRepository.save(user);
            });
    }

    @GetMapping("/decks")
    public ResponseEntity<List<FlashcardDeck>> getAllDecks() {
        User user = getCurrentUser();
        List<FlashcardDeck> decks = deckService.getUserDecks(user.getId());
        return ResponseEntity.ok(decks);
    }

    @GetMapping("/decks/{deckId}")
    public ResponseEntity<FlashcardDeck> getDeck(@PathVariable UUID deckId) {
        User user = getCurrentUser();
        FlashcardDeck deck = deckService.getDeck(deckId, user.getId());
        return ResponseEntity.ok(deck);
    }

    @PostMapping("/decks")
    public ResponseEntity<FlashcardDeck> createDeck(@Valid @RequestBody FlashcardDeckCreateRequest request) {
        User user = getCurrentUser();
        FlashcardDeck deck = deckService.createDeck(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(deck);
    }

    @PutMapping("/decks/{deckId}")
    public ResponseEntity<FlashcardDeck> updateDeck(
            @PathVariable UUID deckId,
            @Valid @RequestBody FlashcardDeckCreateRequest request) {
        User user = getCurrentUser();
        FlashcardDeck deck = deckService.updateDeck(deckId, request, user.getId());
        return ResponseEntity.ok(deck);
    }

    @DeleteMapping("/decks/{deckId}")
    public ResponseEntity<Void> deleteDeck(@PathVariable UUID deckId) {
        User user = getCurrentUser();
        deckService.deleteDeck(deckId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/decks/{deckId}/study")
    public ResponseEntity<Void> startStudySession(@PathVariable UUID deckId) {
        User user = getCurrentUser();
        deckService.recordStudySession(deckId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/decks/search")
    public ResponseEntity<List<FlashcardDeck>> searchDecks(@RequestParam String query) {
        User user = getCurrentUser();
        List<FlashcardDeck> decks = deckService.searchDecks(user.getId(), query);
        return ResponseEntity.ok(decks);
    }

    @GetMapping("/decks/stats")
    public ResponseEntity<FlashcardDeckService.DeckGlobalStats> getGlobalStats() {
        User user = getCurrentUser();
        FlashcardDeckService.DeckGlobalStats stats = deckService.getGlobalStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/decks/{deckId}/cards")
    public ResponseEntity<List<Flashcard>> getFlashcards(@PathVariable UUID deckId) {
        User user = getCurrentUser();
        List<Flashcard> cards = flashcardService.getFlashcardsByDeck(deckId, user.getId());
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/decks/{deckId}/cards")
    public ResponseEntity<Flashcard> createFlashcard(
            @PathVariable UUID deckId,
            @Valid @RequestBody FlashcardCreateRequest request) {
        User user = getCurrentUser();
        Flashcard card = flashcardService.createFlashcard(deckId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<Flashcard> updateFlashcard(
            @PathVariable UUID cardId,
            @Valid @RequestBody FlashcardCreateRequest request) {
        User user = getCurrentUser();
        Flashcard card = flashcardService.updateFlashcard(cardId, request, user.getId());
        return ResponseEntity.ok(card);
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteFlashcard(@PathVariable UUID cardId) {
        User user = getCurrentUser();
        flashcardService.deleteFlashcard(cardId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{cardId}/review")
    public ResponseEntity<Flashcard> reviewFlashcard(
            @PathVariable UUID cardId,
            @RequestBody Map<String, Boolean> body) {
        User user = getCurrentUser();
        Boolean wasCorrect = body.get("wasCorrect");
        Flashcard card = flashcardService.reviewFlashcard(cardId, wasCorrect, user.getId());
        return ResponseEntity.ok(card);
    }

    @GetMapping("/decks/{deckId}/study-session")
    public ResponseEntity<List<Flashcard>> getStudySession(
            @PathVariable UUID deckId,
            @RequestParam(defaultValue = "10") int numberOfCards) {
        User user = getCurrentUser();
        List<Flashcard> cards = flashcardService.getStudySession(deckId, numberOfCards, user.getId());
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/decks/{deckId}/search")
    public ResponseEntity<List<Flashcard>> searchFlashcards(
            @PathVariable UUID deckId,
            @RequestParam String query) {
        User user = getCurrentUser();
        List<Flashcard> cards = flashcardService.searchFlashcards(deckId, query, user.getId());
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/decks/{deckId}/stats")
    public ResponseEntity<FlashcardService.FlashcardStats> getDeckStats(@PathVariable UUID deckId) {
        User user = getCurrentUser();
        FlashcardService.FlashcardStats stats = flashcardService.getFlashcardStats(deckId, user.getId());
        return ResponseEntity.ok(stats);
    }

    // ==================== EXCEPTION HANDLER ====================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        ex.printStackTrace(); // Log per debug
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }
}