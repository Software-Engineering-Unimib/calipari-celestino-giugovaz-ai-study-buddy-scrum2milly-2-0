package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.quiz.QuizAnswerRequest;
import com.ai.studybuddy.dto.quiz.QuizGenerateRequest;
import com.ai.studybuddy.dto.quiz.QuizResultResponse;
import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.mapper.QuizMapper;
import com.ai.studybuddy.model.quiz.Question;
import com.ai.studybuddy.model.quiz.Quiz;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.QuestionRepository;
import com.ai.studybuddy.repository.QuizRepository;
import com.ai.studybuddy.service.inter.AIService;
import com.ai.studybuddy.service.inter.QuizService;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QuizServiceImpl implements QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizServiceImpl.class);

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AIService aiService;
    private final QuizMapper quizMapper;
    private final Gson gson = new Gson();
    
    private QuizService selfProxy; // Campo per l'auto-iniezione

    public QuizServiceImpl(QuizRepository quizRepository,
                           QuestionRepository questionRepository,
                           AIService aiService,
                           QuizMapper quizMapper) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.aiService = aiService;
        this.quizMapper = quizMapper;
    }

    // Auto-iniezione del proxy (con @Lazy per evitare problemi di ciclo)
    @Autowired
    public void setSelfProxy(@Lazy QuizService quizService) {
        this.selfProxy = quizService;
    }

    @Override
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
                request.getDifficultyLevel()
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

    @Override
    @Deprecated
    public Quiz generateQuiz(String topic, int numberOfQuestions, String difficulty, User user) {
        QuizGenerateRequest request = QuizGenerateRequest.builder()
                .topic(topic)
                .numberOfQuestions(numberOfQuestions)
                .difficultyLevel(DifficultyLevel.fromString(difficulty))
                .build();
        // Usa selfProxy invece di this per assicurarti che le transazioni funzionino
        return selfProxy.generateQuiz(request, user);
    }

    @Override
    @Transactional
    public Quiz startQuiz(UUID quizId, UUID userId) {
        Quiz quiz = findQuizByIdAndUser(quizId, userId);
        quiz.start();
        return quizRepository.save(quiz);
    }

    @Override
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

    @Override
    public Quiz getQuiz(UUID quizId, UUID userId) {
        return findQuizByIdAndUser(quizId, userId);
    }

    @Override
    public List<Quiz> getUserQuizzes(UUID userId) {
        return quizRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Quiz> getCompletedQuizzes(UUID userId) {
        return quizRepository.findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId);
    }

    @Override
    public List<Quiz> getPendingQuizzes(UUID userId) {
        return quizRepository.findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Quiz> searchByTopic(UUID userId, String topic) {
        return quizRepository.findByUserIdAndTopicContainingIgnoreCaseOrderByCreatedAtDesc(userId, topic);
    }

    @Override
    public List<Quiz> getBySubject(UUID userId, String subject) {
        return quizRepository.findByUserIdAndSubjectOrderByCreatedAtDesc(userId, subject);
    }

    @Override
    @Transactional
    public void deleteQuiz(UUID quizId, UUID userId) {
        Quiz quiz = findQuizByIdAndUser(quizId, userId);
        quizRepository.delete(quiz);
        log.info("Quiz eliminato: {}", quizId);
    }

    @Override
    @Transactional
    public Quiz retryQuiz(UUID quizId, UUID userId) {
        Quiz quiz = findQuizByIdAndUser(quizId, userId);
        quiz.resetAllAnswers();
        return quizRepository.save(quiz);
    }

    @Override
    public QuizService.QuizStats getUserStats(UUID userId) {
        long totalQuizzes = quizRepository.countByUserId(userId);
        long completedQuizzes = quizRepository.countByUserIdAndIsCompletedTrue(userId);
        Double averageScore = quizRepository.getAverageScoreByUserId(userId);
        List<Quiz> passedQuizzes = quizRepository.findPassedQuizzes(userId);
        List<Quiz> failedQuizzes = quizRepository.findFailedQuizzes(userId);

        return new QuizService.QuizStats(
                totalQuizzes,
                completedQuizzes,
                passedQuizzes.size(),
                failedQuizzes.size(),
                averageScore != null ? averageScore : 0.0
        );
    }

    @Override
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
}