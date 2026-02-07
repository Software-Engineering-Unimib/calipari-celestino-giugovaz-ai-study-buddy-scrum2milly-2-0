package com.ai.studybuddy.dto.quiz;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO per la risposta del risultato di un quiz completato
 */
public class QuizResultResponse {

    private UUID quizId;
    private String topic;
    private String subject;
    private int score;
    private int totalQuestions;
    private double scorePercentage;
    private boolean passed;
    private String feedback;
    private List<QuestionResult> questionResults;

    // ==================== CAMPI GAMIFICATION ====================

    private int xpEarned;
    private int totalXp;
    private int level;
    private boolean leveledUp;
    private List<Map<String, Object>> newBadges;

    // ==================== COSTRUTTORI ====================

    public QuizResultResponse() {}

    public QuizResultResponse(UUID quizId, int score, int totalQuestions, boolean passed) {
        this.quizId = quizId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.passed = passed;
        this.scorePercentage = totalQuestions > 0 ? (double) score / totalQuestions * 100 : 0;
    }

    // ==================== INNER CLASS ====================

    public static class QuestionResult {
        private UUID questionId;
        private String questionText;
        private String userAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private String explanation;

        public QuestionResult() {}

        public QuestionResult(UUID questionId, String questionText, String userAnswer,
                              String correctAnswer, boolean isCorrect, String explanation) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.userAnswer = userAnswer;
            this.correctAnswer = correctAnswer;
            this.isCorrect = isCorrect;
            this.explanation = explanation;
        }

        // Getters & Setters
        public UUID getQuestionId() { return questionId; }
        public void setQuestionId(UUID questionId) { this.questionId = questionId; }
        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
        public String getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
        public boolean isCorrect() { return isCorrect; }
        public void setCorrect(boolean correct) { isCorrect = correct; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    // ==================== GETTERS & SETTERS ====================

    public UUID getQuizId() {
        return quizId;
    }

    public void setQuizId(UUID quizId) {
        this.quizId = quizId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public double getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(double scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public List<QuestionResult> getQuestionResults() {
        return questionResults;
    }

    public void setQuestionResults(List<QuestionResult> questionResults) {
        this.questionResults = questionResults;
    }

    // ==================== GAMIFICATION GETTERS & SETTERS ====================

    public int getXpEarned() {
        return xpEarned;
    }

    public void setXpEarned(int xpEarned) {
        this.xpEarned = xpEarned;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isLeveledUp() {
        return leveledUp;
    }

    public void setLeveledUp(boolean leveledUp) {
        this.leveledUp = leveledUp;
    }

    public List<Map<String, Object>> getNewBadges() {
        return newBadges;
    }

    public void setNewBadges(List<Map<String, Object>> newBadges) {
        this.newBadges = newBadges;
    }
}