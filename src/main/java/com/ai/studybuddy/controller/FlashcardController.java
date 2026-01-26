package com.ai.studybuddy.controller;

import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.dto.FlashcardCreateRequest;
import com.ai.studybuddy.dto.FlashcardDeckCreateRequest;
import com.ai.studybuddy.model.Flashcard;
import com.ai.studybuddy.model.FlashcardDeck;
import com.ai.studybuddy.model.User;
import com.ai.studybuddy.service.FlashcardDeckService;
import com.ai.studybuddy.service.FlashcardService;
import com.ai.studybuddy.service.impl.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {

    private static final Logger logger = LoggerFactory.getLogger(FlashcardController.class);

    @Autowired
    private FlashcardService flashcardService;

    @Autowired
    private FlashcardDeckService deckService;

    @Autowired
    private UserService userService;

    // ==================== DECK ENDPOINTS ====================

    @GetMapping("/decks")
    public ResponseEntity<List<FlashcardDeck>> getAllDecks(Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Recupero deck per utente: {}", user.getEmail());

        List<FlashcardDeck> decks = deckService.getUserDecks(user.getId());
        return ResponseEntity.ok(decks);
    }

    @GetMapping("/decks/{deckId}")
    public ResponseEntity<FlashcardDeck> getDeck(@PathVariable UUID deckId, Principal principal) {
        User user = userService.getCurrentUser(principal);
        FlashcardDeck deck = deckService.getDeck(deckId, user.getId());
        return ResponseEntity.ok(deck);
    }

    @PostMapping("/decks")
    public ResponseEntity<FlashcardDeck> createDeck(
            @Valid @RequestBody FlashcardDeckCreateRequest request,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Creazione deck '{}' per utente: {}", request.getName(), user.getEmail());

        FlashcardDeck deck = deckService.createDeck(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(deck);
    }

    @PutMapping("/decks/{deckId}")
    public ResponseEntity<FlashcardDeck> updateDeck(
            @PathVariable UUID deckId,
            @Valid @RequestBody FlashcardDeckCreateRequest request,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        FlashcardDeck deck = deckService.updateDeck(deckId, request, user.getId());
        return ResponseEntity.ok(deck);
    }

    @DeleteMapping("/decks/{deckId}")
    public ResponseEntity<Void> deleteDeck(@PathVariable UUID deckId, Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Eliminazione deck {} per utente: {}", deckId, user.getEmail());

        deckService.deleteDeck(deckId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/decks/{deckId}/study")
    public ResponseEntity<Void> startStudySession(@PathVariable UUID deckId, Principal principal) {
        User user = userService.getCurrentUser(principal);
        deckService.recordStudySession(deckId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/decks/search")
    public ResponseEntity<List<FlashcardDeck>> searchDecks(
            @RequestParam String query,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        List<FlashcardDeck> decks = deckService.searchDecks(user.getId(), query);
        return ResponseEntity.ok(decks);
    }

    @GetMapping("/decks/stats")
    public ResponseEntity<FlashcardDeckService.DeckGlobalStats> getGlobalStats(Principal principal) {
        User user = userService.getCurrentUser(principal);
        FlashcardDeckService.DeckGlobalStats stats = deckService.getGlobalStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    // ==================== FLASHCARD ENDPOINTS ====================

    @GetMapping("/decks/{deckId}/cards")
    public ResponseEntity<List<Flashcard>> getFlashcards(
            @PathVariable UUID deckId,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        List<Flashcard> cards = flashcardService.getFlashcardsByDeck(deckId, user.getId());
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/decks/{deckId}/cards")
    public ResponseEntity<Flashcard> createFlashcard(
            @PathVariable UUID deckId,
            @Valid @RequestBody FlashcardCreateRequest request,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        Flashcard card = flashcardService.createFlashcard(deckId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @PutMapping("/cards/{cardId}")
    public ResponseEntity<Flashcard> updateFlashcard(
            @PathVariable UUID cardId,
            @Valid @RequestBody FlashcardCreateRequest request,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        Flashcard card = flashcardService.updateFlashcard(cardId, request, user.getId());
        return ResponseEntity.ok(card);
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> deleteFlashcard(@PathVariable UUID cardId, Principal principal) {
        User user = userService.getCurrentUser(principal);
        flashcardService.deleteFlashcard(cardId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cards/{cardId}/review")
    public ResponseEntity<Flashcard> reviewFlashcard(
            @PathVariable UUID cardId,
            @RequestBody Map<String, Boolean> body,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        Boolean wasCorrect = body.get("wasCorrect");
        Flashcard card = flashcardService.reviewFlashcard(cardId, wasCorrect, user.getId());
        return ResponseEntity.ok(card);
    }

    @GetMapping("/decks/{deckId}/study-session")
    public ResponseEntity<List<Flashcard>> getStudySession(
            @PathVariable UUID deckId,
            @RequestParam(defaultValue = "10") int numberOfCards,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        List<Flashcard> cards = flashcardService.getStudySession(deckId, numberOfCards, user.getId());
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/decks/{deckId}/search")
    public ResponseEntity<List<Flashcard>> searchFlashcards(
            @PathVariable UUID deckId,
            @RequestParam String query,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        List<Flashcard> cards = flashcardService.searchFlashcards(deckId, query, user.getId());
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/decks/{deckId}/stats")
    public ResponseEntity<FlashcardService.FlashcardStats> getDeckStats(
            @PathVariable UUID deckId,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        FlashcardService.FlashcardStats stats = flashcardService.getFlashcardStats(deckId, user.getId());
        return ResponseEntity.ok(stats);
    }
}