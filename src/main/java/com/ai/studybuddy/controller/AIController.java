package com.ai.studybuddy.controller;

import com.ai.studybuddy.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

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

    //Test to verify if the problem is Spring Security or the API call to Gemini
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
}