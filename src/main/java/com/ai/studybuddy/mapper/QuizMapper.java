package com.ai.studybuddy.mapper;

import com.ai.studybuddy.dto.quiz.QuizGenerateRequest;
import com.ai.studybuddy.dto.quiz.QuizResultResponse;
import com.ai.studybuddy.model.quiz.Question;
import com.ai.studybuddy.model.quiz.Quiz;
import com.ai.studybuddy.model.user.User;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper per conversioni Quiz DTO <-> Entity
 */
@Component
public class QuizMapper {

    /**
     * Crea Quiz entity da request e user
     */
    public Quiz toEntity(QuizGenerateRequest request, User user) {
        Quiz quiz = new Quiz();
        quiz.setTitle("Quiz: " + request.getTopic());
        quiz.setTopic(request.getTopic());
        quiz.setSubject(request.getSubject());
        quiz.setDifficultyLevel(request.getDifficultyLevel());
        quiz.setNumberOfQuestions(request.getNumberOfQuestions());
        quiz.setUser(user);
        quiz.setIsAiGenerated(true);
        return quiz;
    }

    /**
     * Crea Question entity da JSON dell'AI
     */
    public Question toQuestionEntity(JsonObject json, Quiz quiz, int order) {
        Question question = new Question();
        question.setQuiz(quiz);
        question.setQuestionOrder(order);
        question.setQuestionText(getJsonString(json, "question"));

        // Parsing opzioni
        if (json.has("options") && json.get("options").isJsonArray()) {
            var options = json.getAsJsonArray("options");
            if (options.size() >= 4) {
                question.setOptionA(options.get(0).getAsString());
                question.setOptionB(options.get(1).getAsString());
                question.setOptionC(options.get(2).getAsString());
                question.setOptionD(options.get(3).getAsString());
            }
        }

        // Estrai la risposta corretta - deve essere solo "A", "B", "C" o "D"
        String correctRaw = getJsonString(json, "correct");
        String correctAnswer = extractLetterAnswer(correctRaw, question);
        question.setCorrectAnswer(correctAnswer);

        // Spiegazione opzionale
        if (json.has("explanation")) {
            question.setExplanation(getJsonString(json, "explanation"));
        }

        return question;
    }

    /**
     * Estrae la lettera della risposta corretta
     * L'AI potrebbe restituire "A", "B", "C", "D" oppure il testo completo della risposta
     */
    private String extractLetterAnswer(String correctRaw, Question question) {
        if (correctRaw == null || correctRaw.isEmpty()) {
            return "A"; // default
        }

        // Se è già una singola lettera A-D, usala
        String upper = correctRaw.trim().toUpperCase();
        if (upper.length() == 1 && "ABCD".contains(upper)) {
            return upper;
        }

        // Altrimenti, cerca quale opzione corrisponde al testo
        String correctText = correctRaw.trim();
        if (correctText.equalsIgnoreCase(question.getOptionA())) return "A";
        if (correctText.equalsIgnoreCase(question.getOptionB())) return "B";
        if (correctText.equalsIgnoreCase(question.getOptionC())) return "C";
        if (correctText.equalsIgnoreCase(question.getOptionD())) return "D";

        // Fallback: controlla se inizia con una lettera seguita da ) o .
        if (upper.matches("^[ABCD][).:].*")) {
            return upper.substring(0, 1);
        }

        // Default
        return "A";
    }

    /**
     * Crea lista di QuestionResult per la risposta
     */
    public List<QuizResultResponse.QuestionResult> toQuestionResults(List<Question> questions) {
        return questions.stream()
                .map(this::toQuestionResult)
                .collect(Collectors.toList());
    }

    /**
     * Crea singolo QuestionResult
     */
    public QuizResultResponse.QuestionResult toQuestionResult(Question question) {
        return new QuizResultResponse.QuestionResult(
                question.getQuestionText(),
                question.getUserAnswerText(),
                question.getCorrectOptionText(),
                Boolean.TRUE.equals(question.getIsCorrect()),
                question.getExplanation()
        );
    }

    // Helper per estrarre stringa da JSON in modo sicuro
    private String getJsonString(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return "";
    }
}