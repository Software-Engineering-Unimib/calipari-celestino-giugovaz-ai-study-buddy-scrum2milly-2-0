package com.ai.studybuddy.integration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Client Groq principale con modello Llama 3.3 70B.
 *
 * Questo è il modello più potente e accurato, usato come default
 * per tutte le richieste AI.
 */
@Component("groqPrimaryClient")
public class PrimaryClient implements AIClient {

    private static final Logger log = LoggerFactory.getLogger(PrimaryClient.class);

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Value("${ai.groq.primary-model:llama-3.3-70b-versatile}")
    private String model;

    private final WebClient webClient;
    private final ResponseParser responseParser;
    private final Gson gson = new Gson();

    // Constructor Injection
    public PrimaryClient(WebClient.Builder webClientBuilder, ResponseParser responseParser) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.groq.com/openai/v1")
                .build();
        this.responseParser = responseParser;
    }

    @Override
    public String generateText(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        JsonObject requestBody = buildRequest(prompt);

        log.info("========================================");
        log.info("Chiamata Groq API - PRIMARY MODEL");
        log.info("Model: {}", model);
        log.info("Timestamp: {}", java.time.LocalDateTime.now());
        log.info("========================================");

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return responseParser.extractContent(response);

        } catch (Exception e) {
            handleException(e);
            throw new RuntimeException("Errore Primary Groq Model: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            String testResponse = generateText("Rispondi solo 'OK'");
            return testResponse != null && !testResponse.isEmpty();
        } catch (Exception e) {
            log.error("Primary Groq Model non disponibile: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getModelName() {
        return model + " (Primary)";
    }

    private JsonObject buildRequest(String prompt) {
        JsonArray messages = new JsonArray();

        // System message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "Sei un assistente AI educativo. Rispondi sempre in italiano con accuratezza e dettaglio.");
        messages.add(systemMessage);

        // User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        // Request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 2048);

        return requestBody;
    }

    private void handleException(Exception e) {
        log.error("Errore nella chiamata a Groq Primary API", e);

        if (e.getMessage() != null) {
            if (e.getMessage().contains("429")) {
                throw new RuntimeException("Rate limit raggiunto sul modello principale");
            }
            if (e.getMessage().contains("401")) {
                throw new RuntimeException("API Key Groq non valida");
            }
            if (e.getMessage().contains("503") || e.getMessage().contains("500")) {
                throw new RuntimeException("Modello principale temporaneamente non disponibile");
            }
        }
    }
}