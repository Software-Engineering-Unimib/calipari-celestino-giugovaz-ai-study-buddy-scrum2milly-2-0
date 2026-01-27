package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.exception.AIServiceException;
import com.ai.studybuddy.exception.AIServiceException.AIErrorType;
import com.ai.studybuddy.service.inter.AIService;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    private static final int DEFAULT_MAX_TOKENS = 2048;
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Value("${ai.groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final WebClient webClient;
    private final Gson gson = new Gson();

    public AIServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.groq.com/openai/v1")
                .build();
    }

    @Override
    public String generateExplanation(String topic, String studentLevel) {
        log.info("Generazione spiegazione per topic: '{}', livello: {}", topic, studentLevel);

        String systemPrompt = "Sei un tutor paziente e chiaro. Rispondi sempre in italiano.";
        String userPrompt = String.format(
                "Spiega '%s' a uno studente di livello %s. " +
                        "Usa esempi concreti e un linguaggio semplice.",
                topic, studentLevel
        );

        return callGroqAPI(systemPrompt, userPrompt);
    }

    @Override
    public String generateQuiz(String topic, int numQuestions, String difficulty) {
        log.info("Generazione quiz - topic: '{}', domande: {}, difficoltà: {}",
                topic, numQuestions, difficulty);

        String systemPrompt = "Sei un generatore di quiz educativi. Rispondi SOLO con JSON valido, senza testo aggiuntivo.";
        String userPrompt = String.format(
                "Genera %d domande a scelta multipla su '%s' con difficoltà %s. " +
                        "Formato JSON richiesto: [{\"question\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correct\": \"A\"}]" +
                        "IMPORTANTE: Il campo 'correct' deve contenere SOLO la lettera della risposta corretta (A, B, C o D), non il testo." +
                        "Rispondi SOLO con l'array JSON, nient'altro.",
                numQuestions, topic, difficulty
        );

        return callGroqAPI(systemPrompt, userPrompt);
    }

    @Override
    public String generateQuiz(String topic, int numQuestions, DifficultyLevel difficulty) {
        return generateQuiz(topic, numQuestions, difficulty.getLevel());
    }

    @Override
    @Deprecated
    public String generateFlashCard(String topic, int numCards, String difficulty) {
        DifficultyLevel level = DifficultyLevel.fromString(difficulty);
        return generateFlashcards(topic, numCards, level);
    }

    @Override
    public String generateFlashcards(String topic, int numCards, DifficultyLevel difficulty) {
        log.info("Generazione flashcards - topic: '{}', carte: {}, difficoltà: {}",
                topic, numCards, difficulty);

        String systemPrompt = "Sei un generatore di flashcards educative. Rispondi SOLO con JSON valido, senza testo aggiuntivo.";
        String userPrompt = String.format(
                "Genera %d flashcards su '%s' con difficoltà %s. " +
                        "Formato JSON richiesto: [{\"front\": \"domanda o concetto\", \"back\": \"risposta o spiegazione\"}]" +
                        "Le flashcards devono essere chiare, concise e utili per il ripasso. " +
                        "Rispondi SOLO con l'array JSON, nient'altro.",
                numCards, topic, difficulty.getLevel()
        );

        return callGroqAPI(systemPrompt, userPrompt);
    }

    @Override
    public String generateFlashcardsWithContext(String topic, int numCards,
                                                DifficultyLevel difficulty, String context) {
        log.info("Generazione flashcards con contesto - topic: '{}', carte: {}", topic, numCards);

        String systemPrompt = "Sei un generatore di flashcards educative. Rispondi SOLO con JSON valido, senza testo aggiuntivo.";
        String userPrompt = String.format(
                "Genera %d flashcards su '%s' con difficoltà %s. " +
                        "Contesto aggiuntivo: %s. " +
                        "Formato JSON richiesto: [{\"front\": \"domanda o concetto\", \"back\": \"risposta o spiegazione\"}]" +
                        "Le flashcards devono essere chiare, concise e utili per il ripasso. " +
                        "Rispondi SOLO con l'array JSON, nient'altro.",
                numCards, topic, difficulty.getLevel(),
                context != null ? context : "nessuno"
        );

        return callGroqAPI(systemPrompt, userPrompt);
    }

    @Override
    public JsonArray parseFlashcardsResponse(String aiResponse) {
        try {
            String cleaned = cleanJsonResponse(aiResponse);
            return gson.fromJson(cleaned, JsonArray.class);
        } catch (JsonSyntaxException e) {
            log.error("Errore parsing risposta AI: {}", e.getMessage());
            throw new AIServiceException(AIErrorType.PARSE_ERROR,
                    "Impossibile interpretare la risposta dell'AI");
        }
    }

    /**
     * Pulisce la risposta JSON dall'AI
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new AIServiceException(AIErrorType.PARSE_ERROR, "Risposta AI vuota");
        }
        return response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    /**
     * Chiamata API Groq (OpenAI-compatible)
     */
    private String callGroqAPI(String systemPrompt, String userPrompt) {
        JsonObject requestBody = buildRequestBody(systemPrompt, userPrompt);

        log.debug("Chiamata Groq API - Model: {}, Timestamp: {}", model, LocalDateTime.now());

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(DEFAULT_TIMEOUT)
                    .block();

            return extractContentFromResponse(response);

        } catch (WebClientResponseException e) {
            handleWebClientException(e);
            throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Errore chiamata API Groq: {}", e.getMessage(), e);

            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                throw new AIServiceException(AIErrorType.TIMEOUT);
            }

            throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE,
                    "Errore chiamata API Groq: " + e.getMessage());
        }
    }

    /**
     * Costruisce il body della richiesta
     */
    private JsonObject buildRequestBody(String systemPrompt, String userPrompt) {
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", DEFAULT_TEMPERATURE);
        requestBody.addProperty("max_tokens", DEFAULT_MAX_TOKENS);

        return requestBody;
    }

    /**
     * Estrae il contenuto dalla risposta
     */
    private String extractContentFromResponse(String response) {
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            return jsonResponse
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (Exception e) {
            log.error("Errore parsing risposta Groq: {}", e.getMessage());
            throw new AIServiceException(AIErrorType.PARSE_ERROR);
        }
    }

    /**
     * Gestisce le eccezioni WebClient
     */
    private void handleWebClientException(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        log.error("Errore API Groq - Status: {}, Body: {}", statusCode, e.getResponseBodyAsString());

        switch (statusCode) {
            case 429:
                throw new AIServiceException(AIErrorType.RATE_LIMIT);
            case 401:
                throw new AIServiceException(AIErrorType.INVALID_API_KEY);
            case 503:
            case 502:
            case 504:
                throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE);
            default:
                throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE,
                        "Errore API: " + e.getMessage());
        }
    }
}
