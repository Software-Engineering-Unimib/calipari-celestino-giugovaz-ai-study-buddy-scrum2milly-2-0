package com.ai.studybuddy.dto.quiz;

import com.ai.studybuddy.model.quiz.Quiz;

import java.util.List;

/**
 * DTO per la risposta con i risultati del quiz
 */
public class QuizResultResponse {

    private boolean success;
    private String message;
    private Quiz quiz;
    private int score;
    private int totalQuestions;
    private double percentage;
    private boolean passed;
    private String timeSpent;
    private List<QuestionResult> questionResults;

    // Costruttore privato per factory methods
    private QuizResultResponse() {}

    // Factory methods
    public static QuizResultResponse success(Quiz quiz, List<QuestionResult> results) {
        QuizResultResponse response = new QuizResultResponse();
        response.success = true;
        response.message = quiz.isPassed() ? "Quiz superato!" : "Quiz completato";
        response.quiz = quiz;
        response.score = quiz.getScore() != null ? quiz.getScore() : 0;
        response.totalQuestions = quiz.getNumberOfQuestions() != null ? quiz.getNumberOfQuestions() : 0;
        response.percentage = quiz.getPercentage() != null ? quiz.getPercentage() : 0.0;
        response.passed = quiz.isPassed();
        response.timeSpent = quiz.getFormattedTime();
        response.questionResults = results;
        return response;
    }

    public static QuizResultResponse error(String message) {
        QuizResultResponse response = new QuizResultResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    // Inner class per risultato singola domanda
    public static class QuestionResult {
        private String questionText;
        private String userAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private String explanation;

        public QuestionResult(String questionText, String userAnswer,
                              String correctAnswer, boolean isCorrect, String explanation) {
            this.questionText = questionText;
            this.userAnswer = userAnswer;
            this.correctAnswer = correctAnswer;
            this.isCorrect = isCorrect;
            this.explanation = explanation;
        }

        // Getters
        public String getQuestionText() { return questionText; }
        public String getUserAnswer() { return userAnswer; }
        public String getCorrectAnswer() { return correctAnswer; }
        public boolean isCorrect() { return isCorrect; }
        public String getExplanation() { return explanation; }
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Quiz getQuiz() { return quiz; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public double getPercentage() { return percentage; }
    public boolean isPassed() { return passed; }
    public String getTimeSpent() { return timeSpent; }
    public List<QuestionResult> getQuestionResults() { return questionResults; }
}