package com.ai.studybuddy.util;

public class Const {

    public final static String W_LOGIN = "The server is active and login is working!";
    public final static String UNIVERSITY = "university";
    public final static String SYSTEM_PROMPT = "You are a patient and clear tutor. Always respond in Italian.";
    public final static String USER_PROMPT = "Explain '%s' to a student with %s level. " +
            "Use concrete examples and simple language.";
    public final static String SYSTEM_QPROMPT = "You are an educational quiz generator. Respond ONLY with valid JSON, without additional text.";
    public final static String QUIZ_PROMPT = "Generate %d multiple choice questions about '%s' with %s difficulty. " +
            "Required JSON format: [{\"question\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correct\": \"A\"}]" +
            "Respond ONLY with the JSON array, nothing else.";
    public final static String ERROR_TOO_MANY_REQUESTS = "Too many requests. Try again in a few seconds.";
    public final static String ERROR_INVALID_KEY = "Invalid API Key. Check the configuration.";
    public final static String ERROR_CALL_API = "Error calling Groq API: ";

}
