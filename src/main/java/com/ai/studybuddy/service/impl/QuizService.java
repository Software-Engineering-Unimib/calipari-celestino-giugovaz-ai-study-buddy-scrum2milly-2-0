package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.quiz.QuizAnswerRequest;
import com.ai.studybuddy.dto.quiz.QuizGenerateRequest;
import com.ai.studybuddy.dto.quiz.QuizResultResponse;
import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.exception.UnauthorizedException;
import com.ai.studybuddy.mapper.QuizMapper;
import com.ai.studybuddy.model.quiz.Question;
import com.ai.studybuddy.model.quiz.Quiz;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.QuestionRepository;
import com.ai.studybuddy.repository.QuizRepository;
import com.ai.studybuddy.service.AIService;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AIService aiService;
    private final QuizMapper quizMapper;
    private final Gson gson = new Gson();

    public QuizService(QuizRepository quizRepository,
                       QuestionRepository questionRepository,
                       AIService aiService,
                       QuizMapper quizMapper) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.aiService = aiService;
        this.quizMapper = quizMapper;
    }

    /**
     * Genera un nuovo quiz con AI e lo salva
     */
    @Transactional
    public Quiz generateQuiz(QuizGenerateRequest request, User user) {
        log.info("Generazione quiz - topic: {}, domande: {}, difficoltà: {}",
                request.getTopic(), request.getNumberOfQuestions(), request.getDifficultyLevel());

        // 1. Crea entity Quiz
        Quiz quiz = quizMapper.toEntity(request, user);
        quiz = quizRepository.save(quiz);

        // 2. Genera domande con AI
        String aiResponse = aiService.generateQuiz(
                request.getTopic(),
                request.getNumberOfQuestions(),
                request.getDifficultyLevel().getLevel()
        );

        // 3. Parsa e salva domande
        JsonArray questionsJson = parseQuizJson(aiResponse);

        for (int i = 0; i < questionsJson.size(); i++) {
            JsonObject questionJson = questionsJson.get(i).getAsJsonObject();
            Question question = quizMapper.toQuestionEntity(questionJson, quiz, i + 1);
            quiz.addQuestion(question);
        }

        quiz = quizRepository.save(quiz);
        log.info("Quiz generato con ID: {}, {} domande", quiz.getId(), quiz.getNumberOfQuestions());

        return quiz;
    }

    /**
     * Genera quiz (metodo legacy per retrocompatibilità)
     */
    @Deprecated
    public Quiz generateQuiz(String topic, int numberOfQuestions, String difficulty, User user) {
        QuizGenerateRequest request = QuizGenerateRequest.builder()
                .topic(topic)
                .numberOfQuestions(numberOfQuestions)
                .difficultyLevel(DifficultyLevel.fromString(difficulty))
                .build();
        return generateQuiz(request, user);
    }

    /**
     * Inizia un quiz (segna l'ora di inizio)
     */
    @Transactional
    public Quiz startQuiz(UUID quizId, UUID userId) {
        Quiz quiz = findQuizByIdAndUser(quizId, userId);
        quiz.start();
        return quizRepository.save(quiz);
    }

    /**
     * Invia le risposte e calcola il punteggio
     */
    @Transactional
    public QuizResultResponse submitAnswers(QuizAnswerRequest request, UUID userId) {
        log.info("Invio risposte quiz: {}", request.getQuizId());

        Quiz quiz = findQuizByIdAndUser(request.getQuizId(), userId);

        if (Boolean.TRUE.equals(quiz.getIsCompleted())) {
            return QuizResultResponse.error("Il quiz è già stato completato");
        }

        // Applica le risposte
        Map<UUID, String> answers = request.getAnswers();
        for (Question question : quiz.getQuestions()) {
            String answer = answers.get(question.getId());
            if (answer != null) {
                question.checkAnswer(answer);
            }
        }

        // Completa e calcola punteggio
        quiz.complete();
        quizRepository.save(quiz);

        // Prepara risposta
        List<QuizResultResponse.QuestionResult> results = quizMapper.toQuestionResults(quiz.getQuestions());

        log.info("Quiz completato - Score: {}/{} ({}%)",
                quiz.getScore(), quiz.getTotalPoints(), quiz.getPercentage());

        return QuizResultResponse.success(quiz, results);
    }

    /**
     * Ottiene un quiz per ID
     */
    public Quiz getQuiz(UUID quizId, UUID userId) {
        return findQuizByIdAndUser(quizId, userId);
    }

    /**
     * Ottiene tutti i quiz di un utente
     */
    public List<Quiz> getUserQuizzes(UUID userId) {
        return quizRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Ottiene i quiz completati di un utente
     */
    public List<Quiz> getCompletedQuizzes(UUID userId) {
        return quizRepository.findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId);
    }

    /**
     * Ottiene i quiz in sospeso di un utente
     */
    public List<Quiz> getPendingQuizzes(UUID userId) {
        return quizRepository.findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Cerca quiz per topic
     */
    public List<Quiz> searchByTopic(UUID userId, String topic) {
        return quizRepository.findByUserIdAndTopicContainingIgnoreCaseOrderByCreatedAtDesc(userId, topic);
    }

    /**
     * Ottiene quiz per materia
     */
    public List<Quiz> getBySubject(UUID userId, String subject) {
        return quizRepository.findByUserIdAndSubjectOrderByCreatedAtDesc(userId, subject);
    }

    /**
     * Elimina un quiz
     */
    @Transactional
    public void deleteQuiz(UUID quizId, UUID userId) {
        Quiz quiz = findQuizByIdAndUser(quizId, userId);
        quizRepository.delete(quiz);
        log.info("Quiz eliminato: {}", quizId);
    }

    /**
     * Ripeti un quiz (resetta le risposte)
     */
    @Transactional
    public Quiz retryQuiz(UUID quizId, UUID userId) {
        Quiz quiz = findQuizByIdAndUser(quizId, userId);
        quiz.resetAllAnswers();
        return quizRepository.save(quiz);
    }

    /**
     * Ottiene statistiche quiz dell'utente
     */
    public QuizStats getUserStats(UUID userId) {
        long totalQuizzes = quizRepository.countByUserId(userId);
        long completedQuizzes = quizRepository.countByUserIdAndIsCompletedTrue(userId);
        Double averageScore = quizRepository.getAverageScoreByUserId(userId);
        List<Quiz> passedQuizzes = quizRepository.findPassedQuizzes(userId);
        List<Quiz> failedQuizzes = quizRepository.findFailedQuizzes(userId);

        return new QuizStats(
                totalQuizzes,
                completedQuizzes,
                passedQuizzes.size(),
                failedQuizzes.size(),
                averageScore != null ? averageScore : 0.0
        );
    }

    /**
     * Ottiene quiz recenti (ultimi 7 giorni)
     */
    public List<Quiz> getRecentQuizzes(UUID userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return quizRepository.findRecentQuizzes(userId, since);
    }

    // ==================== HELPER METHODS ====================

    private Quiz findQuizByIdAndUser(UUID quizId, UUID userId) {
        return quizRepository.findByIdAndUserId(quizId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
    }

    private JsonArray parseQuizJson(String aiResponse) {
        String cleaned = aiResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
        return gson.fromJson(cleaned, JsonArray.class);
    }

    // ==================== INNER CLASSES ====================

    /**
     * Statistiche quiz utente
     */
    public static class QuizStats {
        private final long totalQuizzes;
        private final long completedQuizzes;
        private final long passedQuizzes;
        private final long failedQuizzes;
        private final double averageScore;
        private final double passRate;

        public QuizStats(long totalQuizzes, long completedQuizzes,
                         long passedQuizzes, long failedQuizzes, double averageScore) {
            this.totalQuizzes = totalQuizzes;
            this.completedQuizzes = completedQuizzes;
            this.passedQuizzes = passedQuizzes;
            this.failedQuizzes = failedQuizzes;
            this.averageScore = averageScore;
            this.passRate = completedQuizzes > 0
                    ? (double) passedQuizzes / completedQuizzes * 100
                    : 0.0;
        }

        public long getTotalQuizzes() { return totalQuizzes; }
        public long getCompletedQuizzes() { return completedQuizzes; }
        public long getPassedQuizzes() { return passedQuizzes; }
        public long getFailedQuizzes() { return failedQuizzes; }
        public double getAverageScore() { return averageScore; }
        public double getPassRate() { return passRate; }
    }
}