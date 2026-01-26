package com.ai.studybuddy.service;


public interface AIService {

    String generateExplanation(String topic, String studentLevel);
    String generateQuiz(String topic, int numQuestions, String difficulty);
}












