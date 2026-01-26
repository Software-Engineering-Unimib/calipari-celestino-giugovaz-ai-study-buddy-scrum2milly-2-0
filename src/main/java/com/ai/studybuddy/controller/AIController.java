package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.GenerateFlashcardsResponse;
import com.ai.studybuddy.model.Flashcard;
import com.ai.studybuddy.model.User;
import com.ai.studybuddy.repository.UserRepository;
import com.ai.studybuddy.service.AIService;
import com.ai.studybuddy.service.FlashcardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private FlashcardService flashcardService;  // ← Usa solo questo
    
    @Autowired
    private UserRepository userRepository;  // ← TEMPORANEO (finché non crei UserService)

    // ← TEMPORANEO (finché non crei UserService)
    private User getCurrentUser() {
        return userRepository.findByEmail("demo@studybuddy.com")
            .orElseGet(() -> {
                User user = new User();
                user.setFirstName("Demo");
                user.setLastName("User");
                user.setEmail("demo@studybuddy.com");
                user.setPasswordHash("demo-hash");
                return userRepository.save(user);
            });
    }

    /**
     * Test endpoint spiegazione
     */
    @GetMapping("/explain")
    public ResponseEntity<String> getExplanation(
            @RequestParam String topic,
            @RequestParam(defaultValue = "università") String level) {
        String explanation = aiService.generateExplanation(topic, level);
        return ResponseEntity.ok(explanation);
    }

    /**
     * Test to verify if the problem is Spring Security or the API call
     */
    @GetMapping("/test")
    public String test() {
        return "Il server è attivo e il login funziona!";
    }

    /**
     * Test endpoint quiz
     */
    @GetMapping("/quiz")
    public ResponseEntity<String> generateQuiz(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int questions,
            @RequestParam(defaultValue = "media") String difficulty) {
        String quiz = aiService.generateQuiz(topic, questions, difficulty);
        return ResponseEntity.ok(quiz);
    }

    /**
     * Test endpoint flashcard
     */
    @GetMapping("/flashcards")
    public ResponseEntity<String> generateFlashCards(
            @RequestParam String topic,
            @RequestParam(defaultValue = "10") int cards,
            @RequestParam(defaultValue = "avanzato") String difficulty) {
        String flashcards = aiService.generateFlashCard(topic, cards, difficulty);
        return ResponseEntity.ok(flashcards);
    }


    /**
     * Genera E salva flashcard in un deck
     * POST /api/ai/generate-and-save-flashcards
     */
    @PostMapping("/generate-and-save-flashcards")
    public ResponseEntity<GenerateFlashcardsResponse> generateAndSaveFlashcards(
            @RequestParam UUID deckId,
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int numberOfCards,
            @RequestParam(defaultValue = "MEDIUM") String difficulty) {
        
        try {
            User user = getCurrentUser();
            
            List<Flashcard> createdCards = flashcardService.generateAndSaveFlashcards(
                deckId, 
                topic, 
                numberOfCards, 
                difficulty, 
                user
            );
            
            return ResponseEntity.ok(new GenerateFlashcardsResponse(
                true,
                createdCards.size() + " flashcards generate e salvate!",
                createdCards
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new GenerateFlashcardsResponse(
                false,
                "Errore: " + e.getMessage(),
                null
            ));
        }
    }
}