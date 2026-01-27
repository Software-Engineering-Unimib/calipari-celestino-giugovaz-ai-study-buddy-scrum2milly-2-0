package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.dto.quiz.QuizAnswerRequest;
import com.ai.studybuddy.dto.quiz.QuizGenerateRequest;
import com.ai.studybuddy.dto.quiz.QuizResultResponse;
import com.ai.studybuddy.model.quiz.Quiz;
import com.ai.studybuddy.model.user.User;

import java.util.List;
import java.util.UUID;

/**
 * Interfaccia per il servizio Quiz
 */
public interface QuizService {

    /**
     * Genera un nuovo quiz con AI e lo salva
     */
    Quiz generateQuiz(QuizGenerateRequest request, User user);

    /**
     * Genera quiz (metodo legacy)
     * @deprecated Usa {@link #generateQuiz(QuizGenerateRequest, User)} invece
     */
    @Deprecated
    Quiz generateQuiz(String topic, int numberOfQuestions, String difficulty, User user);

    /**
     * Inizia un quiz (segna l'ora di inizio)
     */
    Quiz startQuiz(UUID quizId, UUID userId);

    /**
     * Invia le risposte e calcola il punteggio
     */
    QuizResultResponse submitAnswers(QuizAnswerRequest request, UUID userId);

    /**
     * Ottiene un quiz per ID
     */
    Quiz getQuiz(UUID quizId, UUID userId);

    /**
     * Ottiene tutti i quiz di un utente
     */
    List<Quiz> getUserQuizzes(UUID userId);

    /**
     * Ottiene i quiz completati di un utente
     */
    List<Quiz> getCompletedQuizzes(UUID userId);

    /**
     * Ottiene i quiz in sospeso di un utente
     */
    List<Quiz> getPendingQuizzes(UUID userId);

    /**
     * Cerca quiz per topic
     */
    List<Quiz> searchByTopic(UUID userId, String topic);

    /**
     * Ottiene quiz per materia
     */
    List<Quiz> getBySubject(UUID userId, String subject);

    /**
     * Elimina un quiz
     */
    void deleteQuiz(UUID quizId, UUID userId);

    /**
     * Ripeti un quiz (resetta le risposte)
     */
    Quiz retryQuiz(UUID quizId, UUID userId);

    /**
     * Ottiene statistiche quiz dell'utente
     */
    QuizStats getUserStats(UUID userId);

    /**
     * Ottiene quiz recenti
     */
    List<Quiz> getRecentQuizzes(UUID userId, int days);

    /**
     * Statistiche quiz utente
     */
    class QuizStats {
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
