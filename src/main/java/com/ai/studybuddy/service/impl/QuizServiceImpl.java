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
import java.util.ArrayList;
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

    private QuizService selfProxy;

    public QuizServiceImpl(QuizRepository quizRepository,
                           QuestionRepository questionRepository,
                           AIService aiService,
                           QuizMapper quizMapper) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.aiService = aiService;
        this.quizMapper = quizMapper;
    }

    @Autowired
    public void setSelfProxy(@Lazy QuizService quizService) {
        this.selfProxy = quizService;
    }

    @Override
    @Transactional
    public Quiz generateQuiz(QuizGenerateRequest request, User user) {
        log.info("Generazione quiz - topic: {}, domande: {}, difficoltà: {}, lingua: {}",
                request.getTopic(), request.getNumberOfQuestions(), 
                request.getDifficultyLevel(), request.getLanguage());

        // 1. Crea entity Quiz
        Quiz quiz = quizMapper.toEntity(request, user);
        quiz = quizRepository.save(quiz);

        // 2. Genera domande con AI (con supporto lingua)
        String aiResponse = aiService.generateQuiz(
                request.getTopic(),
                request.getNumberOfQuestions(),
                request.getDifficultyLevel(),
                request.getLanguage()
        );

        // 3. Parsa e salva domande
        JsonArray questionsJson = parseQuizJson(aiResponse);

        for (int i = 0; i < questionsJson.size(); i++) {
            JsonObject questionJson = questionsJson.get(i).getAsJsonObject();
            Question question = quizMapper.toQuestionEntity(questionJson, quiz, i + 1);
            quiz.addQuestion(question);
        }

        quiz = quizRepository.save(quiz);
        log.info("Quiz generato con ID: {}, {} domande, lingua: {}", 
                quiz.getId(), quiz.getNumberOfQuestions(), request.getLanguage());

        return quiz;
    }

    @Override
    @Deprecated
    public Quiz generateQuiz(String topic, int numberOfQuestions, String difficulty, User user) {
        QuizGenerateRequest request = QuizGenerateRequest.builder()
                .topic(topic)
                .numberOfQuestions(numberOfQuestions)
                .difficultyLevel(DifficultyLevel.fromString(difficulty))
                .language("it") // Default italiano per retrocompatibilità
                .build();
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

        // Verifica se già completato
        if (Boolean.TRUE.equals(quiz.getIsCompleted())) {
            log.warn("Quiz {} già completato", request.getQuizId());
            // Restituisce comunque il risultato esistente
            return buildQuizResultResponse(quiz);
        }

        // Applica le risposte
        Map<UUID, String> answers = request.getAnswers();
        int correctCount = 0;

        for (Question question : quiz.getQuestions()) {
            String answer = answers.get(question.getId());
            if (answer != null) {
                question.setUserAnswer(answer);
                boolean isCorrect = question.checkAnswer(answer);
                if (isCorrect) {
                    correctCount++;
                }
            }
        }

        // Completa e calcola punteggio
        quiz.complete();
        quiz = quizRepository.save(quiz);

        log.info("Quiz completato - Score: {}/{} ({}%)",
                quiz.getScore(), quiz.getTotalPoints(), quiz.getPercentage());

        return buildQuizResultResponse(quiz);
    }

    /**
     * Costruisce il QuizResultResponse dal Quiz completato
     * Con supporto multilingua per i feedback
     */
    private QuizResultResponse buildQuizResultResponse(Quiz quiz) {
        QuizResultResponse response = new QuizResultResponse();

        response.setQuizId(quiz.getId());
        response.setTopic(quiz.getTopic());
        response.setSubject(quiz.getSubject());
        response.setScore(quiz.getScore() != null ? quiz.getScore() : 0);
        response.setTotalQuestions(quiz.getNumberOfQuestions() != null ? quiz.getNumberOfQuestions() : 0);

        // Calcola percentuale
        double percentage = 0;
        if (quiz.getNumberOfQuestions() != null && quiz.getNumberOfQuestions() > 0) {
            percentage = (double) response.getScore() / response.getTotalQuestions() * 100;
        }
        response.setScorePercentage(percentage);

        // Quiz superato se >= 60%
        boolean passed = percentage >= 60;
        response.setPassed(passed);

        // Feedback multilingua
        String language = quiz.getLanguage() != null ? quiz.getLanguage() : "it";
        String feedback = generateFeedback(percentage, language);
        response.setFeedback(feedback);

        // Risultati delle singole domande
        List<QuizResultResponse.QuestionResult> questionResults = new ArrayList<>();
        for (Question q : quiz.getQuestions()) {
            QuizResultResponse.QuestionResult qr = new QuizResultResponse.QuestionResult();
            qr.setQuestionId(q.getId());
            qr.setQuestionText(q.getQuestionText());
            qr.setUserAnswer(q.getUserAnswer());
            qr.setCorrectAnswer(q.getCorrectAnswer());
            qr.setCorrect(Boolean.TRUE.equals(q.getIsCorrect()));
            qr.setExplanation(q.getExplanation());
            questionResults.add(qr);
        }
        response.setQuestionResults(questionResults);

        return response;
    }

    /**
     * Genera feedback multilingua basato sul punteggio
     */
    private String generateFeedback(double percentage, String language) {
        switch (language.toLowerCase()) {
            case "en":
                if (percentage >= 90) {
                    return "Excellent! 🏆 Great mastery of the topic!";
                } else if (percentage >= 70) {
                    return "Very good! 👍 Keep it up!";
                } else if (percentage >= 60) {
                    return "Good! ✅ You passed the quiz.";
                } else if (percentage >= 40) {
                    return "Almost! 📚 Review a bit and try again.";
                } else {
                    return "Needs improvement 💪 I recommend reviewing the topic.";
                }
                
            case "es":
                if (percentage >= 90) {
                    return "¡Excelente! 🏆 ¡Gran dominio del tema!";
                } else if (percentage >= 70) {
                    return "¡Muy bien! 👍 ¡Sigue así!";
                } else if (percentage >= 60) {
                    return "¡Bien! ✅ Has aprobado el cuestionario.";
                } else if (percentage >= 40) {
                    return "¡Casi! 📚 Repasa un poco y vuelve a intentarlo.";
                } else {
                    return "Necesita mejorar 💪 Te recomiendo repasar el tema.";
                }
                
            case "fr":
                if (percentage >= 90) {
                    return "Excellent ! 🏆 Très bonne maîtrise du sujet !";
                } else if (percentage >= 70) {
                    return "Très bien ! 👍 Continuez comme ça !";
                } else if (percentage >= 60) {
                    return "Bien ! ✅ Vous avez réussi le quiz.";
                } else if (percentage >= 40) {
                    return "Presque ! 📚 Revoyez un peu et réessayez.";
                } else {
                    return "À améliorer 💪 Je vous recommande de revoir le sujet.";
                }
                
            case "de":
                if (percentage >= 90) {
                    return "Ausgezeichnet! 🏆 Großartige Beherrschung des Themas!";
                } else if (percentage >= 70) {
                    return "Sehr gut! 👍 Weiter so!";
                } else if (percentage >= 60) {
                    return "Gut! ✅ Du hast das Quiz bestanden.";
                } else if (percentage >= 40) {
                    return "Fast! 📚 Ein bisschen nacharbeiten und erneut versuchen.";
                } else {
                    return "Verbesserungsbedarf 💪 Ich empfehle, das Thema nochmals zu wiederholen.";
                }
                
            case "pt":
                if (percentage >= 90) {
                    return "Excelente! 🏆 Grande domínio do tópico!";
                } else if (percentage >= 70) {
                    return "Muito bom! 👍 Continue assim!";
                } else if (percentage >= 60) {
                    return "Bom! ✅ Você passou no questionário.";
                } else if (percentage >= 40) {
                    return "Quase! 📚 Revisa um pouco e tenta novamente.";
                } else {
                    return "Precisa melhorar 💪 Recomendo revisar o tópico.";
                }
                
            default: // Italiano (default)
                if (percentage >= 90) {
                    return "Eccellente! 🏆 Ottima padronanza dell'argomento!";
                } else if (percentage >= 70) {
                    return "Molto bene! 👍 Continua così!";
                } else if (percentage >= 60) {
                    return "Buono! ✅ Hai superato il quiz.";
                } else if (percentage >= 40) {
                    return "Quasi! 📚 Ripassa un po' e riprova.";
                } else {
                    return "Da migliorare 💪 Ti consiglio di ripassare l'argomento.";
                }
        }
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