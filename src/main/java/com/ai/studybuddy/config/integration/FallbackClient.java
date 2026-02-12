package com.ai.studybuddy.config.integration;

import com.ai.studybuddy.exception.AIServiceException;
import com.ai.studybuddy.exception.AIServiceException.AIErrorType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Client Groq di fallback con logging dettagliato per debug
 */
@Component("groqFallbackClient")
public class FallbackClient implements AIClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackClient.class);
    private final ResponseParser responseParser;

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Value("${ai.groq.fallback-model:llama-3.1-8b-instant}")
    private String model;

    private final WebClient webClient;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    //Constructor Injection
    public FallbackClient(WebClient.Builder webClientBuilder, ResponseParser responseParser) {
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

        log.info("========================================");
        log.info("Chiamata Groq API - FALLBACK MODEL");
        log.info("Model: {}", model);
        log.info("API Key presente: {}", apiKey != null && !apiKey.isEmpty());
        log.info("API Key length: {}", apiKey != null ? apiKey.length() : 0);
        log.info("========================================");

        // Costruisci richiesta con system e user prompt separati
        JsonObject requestBody = buildRequestFromPrompt(prompt);

        // LOG DELLA RICHIESTA COMPLETA (senza API key)
        if (log.isInfoEnabled()) {
            log.info("📤 REQUEST BODY:");
            log.info("{}", gson.toJson(requestBody));
        }

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(gson.toJson(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            log.info("✅ Risposta ricevuta con successo");
            return responseParser.extractContent(response);

        } catch (WebClientResponseException e) {
            throw handleWebClientException(e);
        } catch (Exception e) {
            throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE,
                    "Errore generico chiamando Groq Fallback API: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            String testResponse = generateText("Rispondi solo 'OK'");
            return testResponse != null && !testResponse.isEmpty();
        } catch (Exception e) {
            log.error("Fallback Groq Model non disponibile: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getModelName() {
        return model + " (Fallback)";
    }

    /**
     * Costruisce la richiesta separando system e user prompt
     */
    private JsonObject buildRequestFromPrompt(String fullPrompt) {
        JsonArray messages = new JsonArray();

        // Cerca di separare system e user prompt
        String systemPrompt = "Sei un assistente AI educativo. Rispondi in italiano.";
        String userPrompt = fullPrompt;

        // Se il prompt contiene "Sei un", probabilmente è già formattato
        if (fullPrompt.contains("Sei un generatore") || fullPrompt.contains("Sei un tutor")) {
            // Il prompt contiene già il system prompt, estraiamolo
            String[] parts = fullPrompt.split("\\. Genera |\\. Spiega ", 2);
            if (parts.length == 2) {
                systemPrompt = parts[0] + ".";
                userPrompt = (fullPrompt.contains("Genera") ? "Genera " : "Spiega ") + parts[1];
            }
        }

        // System message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        // User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);

        // Request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.5);
        requestBody.addProperty("max_tokens", 1500);

        return requestBody;
    }

    /**
     * Gestisce gli errori HTTP
     */
    private AIServiceException handleWebClientException(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();

        switch (statusCode) {
            case 429:
                return new AIServiceException(AIErrorType.RATE_LIMIT, 
                        "Rate limit Groq API Fallback. Status: " + statusCode);
            case 401:
                return new AIServiceException(AIErrorType.INVALID_API_KEY, 
                        "API Key non valida per Groq Fallback. Status: " + statusCode);
            case 400:
                return new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE,
                        "Bad Request Groq API Fallback. Status: " + statusCode + " - Response: " + responseBody);
            case 502, 503, 504:
                return new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE,
                        "Servizio Groq Fallback non disponibile. Status: " + statusCode);
            default:
                return new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE,
                        "Errore HTTP Groq Fallback: " + statusCode + " - " + responseBody, e);
        }
    }
}