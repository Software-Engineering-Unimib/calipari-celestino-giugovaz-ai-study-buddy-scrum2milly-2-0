package com.ai.studybuddy.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AIService {

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Value("${ai.groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final WebClient webClient;
    private final Gson gson = new Gson();

    public AIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.groq.com/openai/v1")
                .build();
    }

    /**
     * Genera spiegazione personalizzata
     */
    public String generateExplanation(String topic, String studentLevel) {
        String systemPrompt = "Sei un tutor paziente e chiaro. Rispondi sempre in italiano.";
        String userPrompt = String.format(
                "Spiega '%s' a uno studente di livello %s. " +
                        "Usa esempi concreti e un linguaggio semplice.",
                topic, studentLevel
        );

        return callGroqAPI(systemPrompt, userPrompt);
    }

    /**
     * Genera quiz
     */
    public String generateQuiz(String topic, int numQuestions, String difficulty) {
        String systemPrompt = "Sei un generatore di quiz educativi. Rispondi SOLO con JSON valido, senza testo aggiuntivo.";
        String userPrompt = String.format(
                "Genera %d domande a scelta multipla su '%s' con difficoltà %s. " +
                        "Formato JSON richiesto: [{\"question\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correct\": \"A\"}]" +
                        "Rispondi SOLO con l'array JSON, nient'altro.",
                numQuestions, topic, difficulty
        );

        return callGroqAPI(systemPrompt, userPrompt);
    }
    /**
     * Genera flashcard
     */
    public String generateFlashCard(String topic, int numCards, String complexity) {
        String systemPrompt = "Sei un generatore di flashcards educative. Rispondi SOLO con JSON valido, senza testo aggiuntivo.";
        String userPrompt = String.format(
                "Genera %d flashcards su '%s' con complessità %s. " +
                        "Formato JSON richiesto: [{\"front\": \"domanda o concetto\", \"back\": \"risposta o spiegazione\"}]" +
                        "Le flashcards devono essere chiare, concise e utili per il ripasso. " +
                        "Rispondi SOLO con l'array JSON, nient'altro.",
                numCards, topic, complexity
        );

        return callGroqAPI(systemPrompt, userPrompt);
    }

    /**
     * Chiamata API Groq (OpenAI-compatible)
     */
    private String callGroqAPI(String systemPrompt, String userPrompt) {

        // Costruzione payload OpenAI-compatible
        JsonArray messages = new JsonArray();

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
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 2048);

        System.out.println("========================================");
        System.out.println("Chiamata Groq API");
        System.out.println("Model: " + model);
        System.out.println("Timestamp: " + java.time.LocalDateTime.now());
        System.out.println("========================================");

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parsing risposta OpenAI-compatible
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            return jsonResponse
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
            e.printStackTrace();

            // Gestione errori specifici
            if (e.getMessage() != null) {
                if (e.getMessage().contains("429")) {
                    throw new RuntimeException("Troppe richieste. Riprova tra qualche secondo.");
                }
                if (e.getMessage().contains("401")) {
                    throw new RuntimeException("API Key non valida. Verifica la configurazione.");
                }
            }

            throw new RuntimeException("Errore chiamata API Groq: " + e.getMessage());
        }
    }
}












