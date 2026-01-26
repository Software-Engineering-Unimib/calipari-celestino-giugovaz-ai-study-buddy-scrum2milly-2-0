package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.GenerateFlashcardsResponse;
import com.ai.studybuddy.model.Flashcard;
import com.ai.studybuddy.model.User;
import com.ai.studybuddy.service.AIService;
import com.ai.studybuddy.service.FlashcardService;
import com.ai.studybuddy.service.impl.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    @Autowired
    private AIService aiService;

    @Autowired
    private FlashcardService flashcardService;

    @Autowired
    private UserService userService;

    // ==================== EXPLANATION ====================

    @GetMapping("/explain")
    public ResponseEntity<String> getExplanation(
            @RequestParam String topic,
            @RequestParam(defaultValue = "università") String level,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Richiesta spiegazione '{}' da utente: {}", topic, user.getEmail());

        String explanation = aiService.generateExplanation(topic, level);
        return ResponseEntity.ok(explanation);
    }

    // ==================== QUIZ ====================

    @GetMapping("/quiz")
    public ResponseEntity<String> generateQuiz(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int questions,
            @RequestParam(defaultValue = "media") String difficulty,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Generazione quiz '{}' ({} domande) per utente: {}", topic, questions, user.getEmail());

        String quiz = aiService.generateQuiz(topic, questions, difficulty);
        return ResponseEntity.ok(quiz);
    }

    // ==================== FLASHCARDS ====================

    @GetMapping("/flashcards")
    public ResponseEntity<String> generateFlashcards(
            @RequestParam String topic,
            @RequestParam(defaultValue = "10") int cards,
            @RequestParam(defaultValue = "avanzato") String difficulty,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Generazione {} flashcard '{}' per utente: {}", cards, topic, user.getEmail());

        String flashcards = aiService.generateFlashCard(topic, cards, difficulty);
        return ResponseEntity.ok(flashcards);
    }

    @PostMapping("/flashcards/generate")
    public ResponseEntity<GenerateFlashcardsResponse> generateAndSaveFlashcards(
            @RequestParam UUID deckId,
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int numberOfCards,
            @RequestParam(defaultValue = "MEDIUM") String difficulty,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        logger.info("Generazione e salvataggio {} flashcard '{}' nel deck {} per utente: {}",
                numberOfCards, topic, deckId, user.getEmail());

        List<Flashcard> createdCards = flashcardService.generateAndSaveFlashcards(
                deckId,
                topic,
                numberOfCards,
                difficulty,
                user
        );

        return ResponseEntity.ok(new GenerateFlashcardsResponse(
                true,
                String.valueOf(createdCards.size()),
                createdCards
        ));
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}