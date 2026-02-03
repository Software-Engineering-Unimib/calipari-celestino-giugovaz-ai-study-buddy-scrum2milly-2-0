package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.flashcard.GenerateFlashcardsResponse;
import com.ai.studybuddy.dto.quiz.QuizAnswerRequest;
import com.ai.studybuddy.dto.quiz.QuizGenerateRequest;
import com.ai.studybuddy.dto.quiz.QuizResultResponse;
import com.ai.studybuddy.model.flashcard.Flashcard;
import com.ai.studybuddy.model.quiz.Quiz;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.impl.AIServiceImpl;
import com.ai.studybuddy.service.impl.FlashcardServiceImpl;
import com.ai.studybuddy.service.inter.QuizService;
import com.ai.studybuddy.service.inter.UserService;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    private final AIServiceImpl aiServiceImpl;
    private final FlashcardServiceImpl flashcardServiceImpl;
    private final QuizService quizService;
    private final UserService userService;

    // Constructor injection (best practice)
    public AIController(AIServiceImpl aiServiceImpl,
                       FlashcardServiceImpl flashcardServiceImpl,
                       QuizService quizService,
                       UserService userService) {
        this.aiServiceImpl = aiServiceImpl;
        this.flashcardServiceImpl = flashcardServiceImpl;
        this.quizService = quizService;
        this.userService = userService;
    }

    // ==================== EXPLANATION ====================

    @GetMapping("/explain")
    public ResponseEntity<String> getExplanation(
            @RequestParam String topic,
            @RequestParam(defaultValue = "università") String level,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Richiesta spiegazione '{}' da utente: {}", topic, user.getEmail());

        String explanation = aiServiceImpl.generateExplanation(topic, level);
        return ResponseEntity.ok(explanation);
    }

    // ==================== QUIZ (NUOVO - SALVA NEL DB) ====================

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
     */
    @PostMapping("/quiz/submit")
    public ResponseEntity<QuizResultResponse> submitQuizAnswers(
            @RequestBody QuizAnswerRequest request,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        logger.info("Invio risposte quiz {} per utente: {}", request.getQuizId(), user.getEmail());

        QuizResultResponse result = quizService.submitAnswers(request, user.getId());

        logger.info("Quiz completato - Score: {}/{}", result.getScore(), result.getTotalQuestions());
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

    /**
     * LEGACY: Genera quiz senza salvare (per retrocompatibilità)
     * @deprecated Usa POST /quiz/generate invece
     */
    @Deprecated
    @GetMapping("/quiz")
    public ResponseEntity<String> generateQuizLegacy(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int questions,
            @RequestParam(defaultValue = "media") String difficulty,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.warn("Uso endpoint deprecato /quiz da utente: {}", user.getEmail());

        String quiz = aiServiceImpl.generateQuiz(topic, questions, difficulty);
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

        String flashcards = aiServiceImpl.generateFlashCard(topic, cards, difficulty);
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

        List<Flashcard> createdCards = flashcardServiceImpl.generateAndSaveFlashcards(
                deckId,
                topic,
                numberOfCards,
                difficulty,
                user
        );

        return ResponseEntity.ok(new GenerateFlashcardsResponse(
                true,
                String.format("Generate %d flashcard con successo", createdCards.size()),
                createdCards
        ));
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}