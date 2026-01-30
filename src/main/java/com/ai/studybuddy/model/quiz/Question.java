package com.ai.studybuddy.model.quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity Question - rappresenta una singola domanda di un quiz
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ==================== RELAZIONI ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnore
    private Quiz quiz;

    // ==================== CONTENUTO ====================

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "option_a", nullable = false, length = 500)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 500)
    private String optionB;

    @Column(name = "option_c", nullable = false, length = 500)
    private String optionC;

    @Column(name = "option_d", nullable = false, length = 500)
    private String optionD;

    @Column(name = "correct_answer", nullable = false, length = 1)
    private String correctAnswer;  // "A", "B", "C" o "D"

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;  // Spiegazione opzionale della risposta

    // ==================== RISPOSTA UTENTE ====================

    @Column(name = "user_answer", length = 1)
    private String userAnswer;  // Risposta data dallo studente

    @Column(name = "is_correct")
    private Boolean isCorrect;  // Se la risposta era corretta

    // ==================== ORDINE ====================

    @Column(name = "question_order")
    private Integer questionOrder;  // Ordine della domanda nel quiz

    // ==================== BUSINESS LOGIC ====================

    /**
     * Verifica se la risposta data è corretta
     */
    public boolean checkAnswer(String answer) {
        if (answer == null || correctAnswer == null) return false;
        this.userAnswer = answer.toUpperCase().trim();
        this.isCorrect = this.userAnswer.equals(correctAnswer.toUpperCase().trim());
        return this.isCorrect;
    }

    /**
     * Ottiene il testo dell'opzione corretta
     */
    public String getCorrectOptionText() {
        return switch (correctAnswer.toUpperCase()) {
            case "A" -> optionA;
            case "B" -> optionB;
            case "C" -> optionC;
            case "D" -> optionD;
            default -> null;
        };
    }

    /**
     * Ottiene il testo dell'opzione selezionata dall'utente
     */
    public String getUserAnswerText() {
        if (userAnswer == null) return null;
        return switch (userAnswer.toUpperCase()) {
            case "A" -> optionA;
            case "B" -> optionB;
            case "C" -> optionC;
            case "D" -> optionD;
            default -> null;
        };
    }

    /**
     * Verifica se la domanda è stata risposta
     */
    public boolean isAnswered() {
        return userAnswer != null && !userAnswer.isBlank();
    }

    /**
     * Resetta la risposta dell'utente
     */
    public void resetAnswer() {
        this.userAnswer = null;
        this.isCorrect = null;
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return Objects.equals(id, question.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ==================== GETTERS & SETTERS ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }
}