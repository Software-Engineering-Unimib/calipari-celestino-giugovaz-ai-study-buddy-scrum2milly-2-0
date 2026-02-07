package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.flashcard.FlashcardCreateRequest;
import com.ai.studybuddy.dto.flashcard.FlashcardDeckCreateRequest;
import com.ai.studybuddy.dto.gamification.GamificationDTO.XpEventResponse;
import com.ai.studybuddy.model.flashcard.Flashcard;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.impl.GamificationServiceImpl;
import com.ai.studybuddy.service.inter.FlashcardDeckService;
import com.ai.studybuddy.service.inter.FlashcardService;
import com.ai.studybuddy.service.inter.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {

    private static final Logger logger = LoggerFactory.getLogger(FlashcardController.class);

    private final FlashcardService flashcardService;
    private final FlashcardDeckService deckService;
    private final UserService userService;
    private final GamificationServiceImpl gamificationService;  // AGGIUNTO

    // Constructor injection
    public FlashcardController(FlashcardService flashcardService,
                               FlashcardDeckService deckService,
                               UserService userService,
                               GamificationServiceImpl gamificationService) {  // AGGIUNTO
        this.flashcardService = flashcardService;
        this.deckService = deckService;
        this.userService = userService;
        this.gamificationService = gamificationService;  // AGGIUNTO
    }

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

    /**
     * Registra una revisione di flashcard
     * ✅ ASSEGNA XP PER FLASHCARD STUDIATA (+2 XP per card)
     */
    @PostMapping("/cards/{cardId}/review")
    public ResponseEntity<Map<String, Object>> reviewFlashcard(
            @PathVariable UUID cardId,
            @RequestBody Map<String, Boolean> body,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        Boolean wasCorrect = body.get("wasCorrect");

        // Registra la revisione
        Flashcard card = flashcardService.reviewFlashcard(cardId, wasCorrect, user.getId());

        // ✅ ASSEGNA XP PER FLASHCARD STUDIATA (+2 XP)
        XpEventResponse xpEvent = gamificationService.recordFlashcardXp(user, 1);

        logger.info("Flashcard {} reviewata da {}, XP guadagnati: {}",
                cardId, user.getEmail(), xpEvent.getXpEarned());

        // Costruisci risposta con info flashcard + XP
        Map<String, Object> response = new HashMap<>();
        response.put("flashcard", card);
        response.put("xpEarned", xpEvent.getXpEarned());
        response.put("totalXp", xpEvent.getNewTotalXp());
        response.put("level", xpEvent.getNewLevel());
        response.put("leveledUp", xpEvent.isLeveledUp());

        if (xpEvent.getNewBadges() != null && !xpEvent.getNewBadges().isEmpty()) {
            List<Map<String, Object>> badgesList = new java.util.ArrayList<>();
            for (var badge : xpEvent.getNewBadges()) {
                Map<String, Object> badgeMap = new HashMap<>();
                badgeMap.put("name", badge.getName());
                badgeMap.put("icon", badge.getIcon());
                badgeMap.put("description", badge.getDescription());
                badgesList.add(badgeMap);
            }
            response.put("newBadges", badgesList);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Completa una sessione di studio di più flashcards
     * ✅ ASSEGNA XP PER TUTTE LE FLASHCARDS STUDIATE
     */
    @PostMapping("/decks/{deckId}/complete-session")
    public ResponseEntity<Map<String, Object>> completeStudySession(
            @PathVariable UUID deckId,
            @RequestBody Map<String, Object> body,
            Principal principal) {

        User user = userService.getCurrentUser(principal);

        // Numero di carte studiate nella sessione
        int cardsStudied = body.containsKey("cardsStudied")
                ? ((Number) body.get("cardsStudied")).intValue()
                : 0;

        // Durata sessione in minuti (opzionale, per future sessioni focus)
        int durationMinutes = body.containsKey("durationMinutes")
                ? ((Number) body.get("durationMinutes")).intValue()
                : 0;

        // Registra la sessione di studio nel deck
        deckService.recordStudySession(deckId, user.getId());

        // ✅ ASSEGNA XP PER TUTTE LE FLASHCARDS STUDIATE
        XpEventResponse xpEvent = null;
        if (cardsStudied > 0) {
            xpEvent = gamificationService.recordFlashcardXp(user, cardsStudied);
            logger.info("Sessione studio completata: {} carte, {} XP guadagnati",
                    cardsStudied, xpEvent.getXpEarned());
        }

        // Costruisci risposta
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cardsStudied", cardsStudied);

        if (xpEvent != null) {
            response.put("xpEarned", xpEvent.getXpEarned());
            response.put("totalXp", xpEvent.getNewTotalXp());
            response.put("level", xpEvent.getNewLevel());
            response.put("leveledUp", xpEvent.isLeveledUp());

            if (xpEvent.getNewBadges() != null && !xpEvent.getNewBadges().isEmpty()) {
                List<Map<String, Object>> badgesList = new java.util.ArrayList<>();
                for (var badge : xpEvent.getNewBadges()) {
                    Map<String, Object> badgeMap = new HashMap<>();
                    badgeMap.put("name", badge.getName());
                    badgeMap.put("icon", badge.getIcon());
                    badgeMap.put("description", badge.getDescription());
                    badgesList.add(badgeMap);
                }
                response.put("newBadges", badgesList);
            }
        }

        return ResponseEntity.ok(response);
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