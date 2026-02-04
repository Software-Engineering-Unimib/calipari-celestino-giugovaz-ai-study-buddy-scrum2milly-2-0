package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.explanation.ExplanationResponse;
import com.ai.studybuddy.dto.flashcard.GenerateFlashcardsResponse;
import com.ai.studybuddy.dto.gamification.GamificationDTO.XpEventResponse;
import com.ai.studybuddy.dto.quiz.QuizAnswerRequest;
import com.ai.studybuddy.dto.quiz.QuizGenerateRequest;
import com.ai.studybuddy.dto.quiz.QuizResultResponse;
import com.ai.studybuddy.model.flashcard.Flashcard;
import com.ai.studybuddy.model.quiz.Quiz;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.impl.FlashcardServiceImpl;
import com.ai.studybuddy.service.impl.GamificationServiceImpl;
import com.ai.studybuddy.service.inter.ExplanationService;
import com.ai.studybuddy.service.inter.QuizService;
import com.ai.studybuddy.service.inter.UserService;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    private final ExplanationService explanationService;
    private final FlashcardServiceImpl flashcardServiceImpl;
    private final QuizService quizService;
    private final UserService userService;
    private final GamificationServiceImpl gamificationService;

    public AIController(ExplanationService explanationService,
                        FlashcardServiceImpl flashcardServiceImpl,
                        QuizService quizService,
                        UserService userService,
                        GamificationServiceImpl gamificationService) {
        this.explanationService = explanationService;
        this.flashcardServiceImpl = flashcardServiceImpl;
        this.quizService = quizService;
        this.userService = userService;
        this.gamificationService = gamificationService;
    }

    // ==================== EXPLANATION ====================

    /**
     * Genera una spiegazione personalizzata
     * ✅ ASSEGNA XP PER SPIEGAZIONE (+10 XP)
     */
    @GetMapping("/explain")
    public ResponseEntity<ExplanationResponse> getExplanation(
            @RequestParam String topic,
            @RequestParam(defaultValue = "università") String level,
            @RequestParam(required = false) String subject,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        logger.info("Richiesta spiegazione '{}' da utente: {}", topic, user.getEmail());

        ExplanationResponse response = explanationService.generateExplanation(topic, level, subject, user);

        logger.info("Spiegazione generata - XP: +{}, Totale: {}",
                response.getXpEarned(), response.getTotalXp());

        return ResponseEntity.ok(response);
    }

    // ==================== QUIZ ====================

    /**
     * Genera un quiz E lo salva nel database
     */
    @PostMapping("/quiz/generate")
    public ResponseEntity<Quiz> generateAndSaveQuiz(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int numberOfQuestions,
            @RequestParam(defaultValue = "INTERMEDIO") String difficulty,
            @RequestParam(required = false) String subject,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        logger.info("Generazione e salvataggio quiz '{}' ({} domande) per utente: {}",
                topic, numberOfQuestions, user.getEmail());

        QuizGenerateRequest request = QuizGenerateRequest.builder()
                .topic(topic)
                .numberOfQuestions(numberOfQuestions)
                .difficultyLevel(DifficultyLevel.fromString(difficulty))
                .subject(subject)
                .build();

        Quiz quiz = quizService.generateQuiz(request, user);

        logger.info("Quiz salvato con ID: {}", quiz.getId());
        return ResponseEntity.ok(quiz);
    }

    /**
     * Inizia un quiz (segna l'ora di inizio)
     */
    @PostMapping("/quiz/{quizId}/start")
    public ResponseEntity<Quiz> startQuiz(
            @PathVariable UUID quizId,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        logger.info("Inizio quiz {} per utente: {}", quizId, user.getEmail());

        Quiz quiz = quizService.startQuiz(quizId, user.getId());
        return ResponseEntity.ok(quiz);
    }

    /**
     * Invia le risposte del quiz e ottieni il risultato
     * ✅ ASSEGNA XP PER QUIZ COMPLETATO (+20 XP base, +10 bonus se superato)
     */
    @PostMapping("/quiz/submit")
    public ResponseEntity<QuizResultResponse> submitQuizAnswers(
            @RequestBody QuizAnswerRequest request,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        logger.info("Invio risposte quiz {} per utente: {}", request.getQuizId(), user.getEmail());

        // Processa le risposte
        QuizResultResponse result = quizService.submitAnswers(request, user.getId());

        // ✅ ASSEGNA XP PER QUIZ COMPLETATO
        boolean passed = result.isPassed();
        String topic = result.getTopic();
        String subject = result.getSubject();
        double scorePercentage = result.getScorePercentage();
        int totalQuestions = result.getTotalQuestions();
        int correctAnswers = result.getScore();

        XpEventResponse xpEvent = gamificationService.recordQuizXp(
                user,
                passed,
                topic,
                subject,
                scorePercentage,
                totalQuestions,
                correctAnswers
        );

        // Aggiungi info XP alla risposta
        result.setXpEarned(xpEvent.getXpEarned());
        result.setTotalXp(xpEvent.getNewTotalXp());
        result.setLevel(xpEvent.getNewLevel());
        result.setLeveledUp(xpEvent.isLeveledUp());

        if (xpEvent.getNewBadges() != null && !xpEvent.getNewBadges().isEmpty()) {
            List<Map<String, Object>> badgesList = new java.util.ArrayList<>();
            for (var badge : xpEvent.getNewBadges()) {
                Map<String, Object> badgeMap = new HashMap<>();
                badgeMap.put("name", badge.getName());
                badgeMap.put("icon", badge.getIcon());
                badgeMap.put("description", badge.getDescription());
                badgeMap.put("xpReward", badge.getXpReward() != null ? badge.getXpReward() : 0);
                badgesList.add(badgeMap);
            }
            result.setNewBadges(badgesList);
        }

        logger.info("Quiz completato - Score: {}/{}, XP guadagnati: {}",
                result.getScore(), result.getTotalQuestions(), xpEvent.getXpEarned());

        return ResponseEntity.ok(result);
    }

    /**
     * Ottieni un quiz specifico
     */
    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<Quiz> getQuiz(
            @PathVariable UUID quizId,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        Quiz quiz = quizService.getQuiz(quizId, user.getId());
        return ResponseEntity.ok(quiz);
    }

    /**
     * Ottieni tutti i quiz dell'utente
     */
    @GetMapping("/quiz/my")
    public ResponseEntity<List<Quiz>> getMyQuizzes(Principal principal) {
        User user = userService.getCurrentUser(principal);
        List<Quiz> quizzes = quizService.getUserQuizzes(user.getId());
        return ResponseEntity.ok(quizzes);
    }

    /**
     * Ottieni quiz completati
     */
    @GetMapping("/quiz/completed")
    public ResponseEntity<List<Quiz>> getCompletedQuizzes(Principal principal) {
        User user = userService.getCurrentUser(principal);
        List<Quiz> quizzes = quizService.getCompletedQuizzes(user.getId());
        return ResponseEntity.ok(quizzes);
    }

    /**
     * Ottieni statistiche quiz
     */
    @GetMapping("/quiz/stats")
    public ResponseEntity<QuizService.QuizStats> getQuizStats(Principal principal) {
        User user = userService.getCurrentUser(principal);
        QuizService.QuizStats stats = quizService.getUserStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * Ripeti un quiz (resetta le risposte)
     */
    @PostMapping("/quiz/{quizId}/retry")
    public ResponseEntity<Quiz> retryQuiz(
            @PathVariable UUID quizId,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        Quiz quiz = quizService.retryQuiz(quizId, user.getId());
        return ResponseEntity.ok(quiz);
    }

    /**
     * Elimina un quiz
     */
    @DeleteMapping("/quiz/{quizId}")
    public ResponseEntity<Void> deleteQuiz(
            @PathVariable UUID quizId,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        quizService.deleteQuiz(quizId, user.getId());
        return ResponseEntity.noContent().build();
    }

    // ==================== FLASHCARDS ====================

    /**
     * Genera e salva flashcards con AI
     * ✅ ASSEGNA XP PER FLASHCARDS GENERATE (+2 XP per card)
     */
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

        List<Flashcard> createdCards = flashcardServiceImpl.generateAndSaveFlashcards(
                deckId,
                topic,
                numberOfCards,
                difficulty,
                user
        );

        // ✅ ASSEGNA XP PER FLASHCARDS GENERATE
        XpEventResponse xpEvent = gamificationService.recordFlashcardXp(user, createdCards.size());

        GenerateFlashcardsResponse response = new GenerateFlashcardsResponse(
                true,
                String.format("Generate %d flashcard con successo (+%d XP)",
                        createdCards.size(), xpEvent.getXpEarned()),
                createdCards
        );

        // Aggiungi info XP alla risposta
        response.setXpEarned(xpEvent.getXpEarned());
        response.setTotalXp(xpEvent.getNewTotalXp());
        response.setLeveledUp(xpEvent.isLeveledUp());

        return ResponseEntity.ok(response);
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}