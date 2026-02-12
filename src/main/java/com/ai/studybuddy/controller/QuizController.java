package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.gamification.GamificationDTO.XpEventResponse;
import com.ai.studybuddy.dto.quiz.QuizAnswerRequest;
import com.ai.studybuddy.dto.quiz.QuizGenerateRequest;
import com.ai.studybuddy.dto.quiz.QuizResultResponse;
import com.ai.studybuddy.model.quiz.Quiz;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.impl.GamificationServiceImpl;
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
@RequestMapping("/api/ai/quiz")
public class QuizController {

    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);

    private final QuizService quizService;
    private final UserService userService;
    private final GamificationServiceImpl gamificationService;

    public QuizController(QuizService quizService,
                          UserService userService,
                          GamificationServiceImpl gamificationService) {
        this.quizService = quizService;
        this.userService = userService;
        this.gamificationService = gamificationService;
    }

    /**
     * Genera un quiz e lo salva nel database
     */
    @PostMapping("/generate")
    public ResponseEntity<Quiz> generateQuiz(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int numberOfQuestions,
            @RequestParam(defaultValue = "INTERMEDIO") String difficulty,
            @RequestParam(required = false) String subject,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        logger.info("Generazione quiz '{}' ({} domande) per utente: {}, lingua: {}",
                topic, numberOfQuestions, user.getEmail(), user.getPreferredLanguage());

        QuizGenerateRequest request = QuizGenerateRequest.builder()
                .topic(topic)
                .numberOfQuestions(numberOfQuestions)
                .difficultyLevel(DifficultyLevel.fromString(difficulty))
                .subject(subject)
                .language(user.getPreferredLanguage())
                .build();

        Quiz quiz = quizService.generateQuiz(request, user);

        logger.info("Quiz salvato con ID: {}", quiz.getId());
        return ResponseEntity.ok(quiz);
    }

    /**
     * Inizia un quiz (segna l'ora di inizio)
     */
    @PostMapping("/{quizId}/start")
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
     * ✅ Assegna XP per quiz completato (+20 XP base, +10 bonus se superato)
     */
    @PostMapping("/submit")
    public ResponseEntity<QuizResultResponse> submitQuizAnswers(
            @RequestBody QuizAnswerRequest request,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        logger.info("Invio risposte quiz {} per utente: {}", request.getQuizId(), user.getEmail());

        QuizResultResponse result = quizService.submitAnswers(request, user.getId());

        // Assegna XP
        XpEventResponse xpEvent = gamificationService.recordQuizXp(
                user,
                result.isPassed(),
                result.getTopic(),
                result.getSubject(),
                result.getScorePercentage(),
                result.getTotalQuestions(),
                result.getScore()
        );

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

        logger.info("Quiz completato - Score: {}/{}, XP: {}",
                result.getScore(), result.getTotalQuestions(), xpEvent.getXpEarned());

        return ResponseEntity.ok(result);
    }

    /**
     * Ottieni un quiz specifico
     */
    @GetMapping("/{quizId}")
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
    @GetMapping("/my")
    public ResponseEntity<List<Quiz>> getMyQuizzes(Principal principal) {
        User user = userService.getCurrentUser(principal);
        return ResponseEntity.ok(quizService.getUserQuizzes(user.getId()));
    }

    /**
     * Ottieni quiz completati
     */
    @GetMapping("/completed")
    public ResponseEntity<List<Quiz>> getCompletedQuizzes(Principal principal) {
        User user = userService.getCurrentUser(principal);
        return ResponseEntity.ok(quizService.getCompletedQuizzes(user.getId()));
    }

    /**
     * Ottieni statistiche quiz
     */
    @GetMapping("/stats")
    public ResponseEntity<QuizService.QuizStats> getQuizStats(Principal principal) {
        User user = userService.getCurrentUser(principal);
        return ResponseEntity.ok(quizService.getUserStats(user.getId()));
    }

    /**
     * Ripeti un quiz
     */
    @PostMapping("/{quizId}/retry")
    public ResponseEntity<Quiz> retryQuiz(
            @PathVariable UUID quizId,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        return ResponseEntity.ok(quizService.retryQuiz(quizId, user.getId()));
    }

    /**
     * Elimina un quiz
     */
    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> deleteQuiz(
            @PathVariable UUID quizId,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        quizService.deleteQuiz(quizId, user.getId());
        return ResponseEntity.noContent().build();
    }
}