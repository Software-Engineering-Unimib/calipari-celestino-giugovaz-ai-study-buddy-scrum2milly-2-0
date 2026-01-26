package com.ai.studybuddy.controller;

import com.ai.studybuddy.service.AIServiceImpl;
import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIServiceImpl aiService;


    //Constructor Injection which is a technique/best-practice to implement Dependency Injection Pattern
    public AIController(AIServiceImpl aiService) {
        this.aiService = aiService;
    }

    /**
     * Test endpoint spiegazione
     */
    @GetMapping("/explain")
    public ResponseEntity<String> getExplanation(
            @RequestParam String topic,
            @RequestParam(defaultValue = Const.UNIVERSITY) String level) {

        String explanation = aiService.generateExplanation(topic, level);
        return ResponseEntity.ok(explanation);
    }

    //Test to verify if the problem is Spring Security or the API call to Gemini
    @GetMapping("/test")
    public String test() {
        return Const.W_LOGIN;
    }

    /**
     * Test endpoint quiz
     */
    @GetMapping("/quiz")
    public ResponseEntity<String> generateQuiz(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int questions,
            @RequestParam(defaultValue = "intermedio") String difficulty) {

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
}
