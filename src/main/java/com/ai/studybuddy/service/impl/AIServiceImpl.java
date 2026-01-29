package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.config.integration.AIClient;
import com.ai.studybuddy.exception.AIServiceException;
import com.ai.studybuddy.exception.AIServiceException.AIErrorType;
import com.ai.studybuddy.service.inter.AIService;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Servizio principale per la generazione di contenuti AI.
 *
 * Implementa una strategia di fallback a 2 livelli:
 * 1. Groq Primary Model (llama-3.3-70b-versatile) - più potente
 * 2. Groq Fallback Model (mixtral-8x7b-32768) - più veloce
 *
 */
@Service
public class AIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    private final AIClient primaryClient;
    private final AIClient fallbackClient;
    private final Gson gson = new Gson();

    @Value("${ai.groq.test-fallback:true}")
    private boolean testFallback;

    public AIServiceImpl(
            @Qualifier("groqPrimaryClient") AIClient primaryClient,
            @Qualifier("groqFallbackClient") AIClient fallbackClient
    ) {
        this.primaryClient = primaryClient;
        this.fallbackClient = fallbackClient;
    }

    // ========================================
    // METODI PUBBLICI - GENERAZIONE CONTENUTI
    // ========================================

    @Override
    public String generateExplanation(String topic, String studentLevel) {


        log.info("Generazione spiegazione per topic: '{}', livello: {}", topic, studentLevel);

        String prompt = buildExplanationPrompt(topic, studentLevel);
        return callAIWithFallback(prompt);
    }

    @Override
    public String generateQuiz(String topic, int numQuestions, String difficulty) {
        log.info("Generazione quiz - topic: '{}', domande: {}, difficoltà: {}",
                topic, numQuestions, difficulty);

        String prompt = buildQuizPrompt(topic, numQuestions, difficulty);
        return callAIWithFallback(prompt);
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

        String prompt = buildFlashcardsPrompt(topic, numCards, difficulty);
        return callAIWithFallback(prompt);
    }

    @Override
    public String generateFlashcardsWithContext(String topic, int numCards,
                                                DifficultyLevel difficulty, String context) {
        log.info("Generazione flashcards con contesto - topic: '{}', carte: {}", topic, numCards);

        String prompt = buildFlashcardsWithContextPrompt(topic, numCards, difficulty, context);
        return callAIWithFallback(prompt);
    }

    @Override
    public JsonArray parseFlashcardsResponse(String aiResponse) {
        try {



            if(aiResponse == null || aiResponse.isEmpty())
                throw new AIServiceException(AIErrorType.RESPONSE_NULL);
            String cleaned = cleanJsonResponse(aiResponse);
            return gson.fromJson(cleaned, JsonArray.class);
        } catch (JsonSyntaxException e) {

            throw new AIServiceException(AIErrorType.PARSE_ERROR,
                    "Impossibile interpretare la risposta dell'AI");
        }
    }

    // ========================================
    // METODI PUBBLICI - UTILITÀ
    // ========================================

    /**
     * Verifica quale modello è disponibile
     */

    //* SERVONO QUESTI METODI getAvailableModel e isAnyModelAvailable PER I TEST



    public String getAvailableModel() {
        if (primaryClient.isAvailable()) {
            return primaryClient.getModelName();
        }
        if (fallbackClient.isAvailable()) {
            return fallbackClient.getModelName();
        }
        return "Nessun modello AI disponibile";
    }


    public boolean isAnyModelAvailable() {
        return primaryClient.isAvailable() || fallbackClient.isAvailable();
    }

    /**
     * Verifica se almeno un modello AI è disponibile

    public boolean isAnyModelAvailable() {
        return primaryClient.isAvailable() || fallbackClient.isAvailable();
    }


    // ========================================
    // METODI PRIVATI - FALLBACK LOGIC
    // ========================================

    /**
     * Chiamata AI con fallback automatico tra modelli
     */
    private String callAIWithFallback(String prompt) {

        // TEST MODE: Forza fallback

        if (testFallback) {
            log.warn("⚠️ TEST MODE ATTIVO: Forzando fallback al modello secondario");
            log.warn("⚠️ Per disattivare: imposta ai.groq.test-fallback=false");
            throw new AIServiceException(AIErrorType.RATE_LIMIT, "Test fallback");
        }


        // LIVELLO 1: Prova con modello principale (Llama 3.3 70B)
        try {


            log.debug("Tentativo con {}", primaryClient.getModelName());
            return primaryClient.generateText(prompt);

        } catch (Exception primaryError) {
            log.warn("Primary model fallito: {}", primaryError.getMessage());

            // LIVELLO 2: Prova con modello di fallback (Mixtral 8x7B)
            try {
                log.info("🔄 Fallback a {}", fallbackClient.getModelName());
                return fallbackClient.generateText(prompt);

            } catch (WebClientResponseException e) {
                handleWebClientException(e);
                throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE);

            } catch (Exception fallbackError) {
                log.error("❌ Anche il fallback model è fallito: {}",
                        fallbackError.getMessage());

                if (fallbackError.getMessage() != null &&
                        fallbackError.getMessage().contains("timeout")) {
                    throw new AIServiceException(AIErrorType.TIMEOUT);
                }

                throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE,
                        "Tutti i modelli AI non disponibili: " + fallbackError.getMessage());
            }
        }
    }

    // ========================================
    // METODI PRIVATI - COSTRUZIONE PROMPT
    // ========================================

    /**
     * Costruisce il prompt per spiegazioni
     */
    private String buildExplanationPrompt(String topic, String studentLevel) {
        return String.format(
                "Sei un tutor paziente e chiaro. Rispondi sempre in italiano. " +
                        "Spiega '%s' a uno studente di livello %s. " +
                        "Usa esempi concreti e un linguaggio semplice.",
                topic, studentLevel
        );
    }

    /**
     * Costruisce il prompt per quiz
     */
    private String buildQuizPrompt(String topic, int numQuestions, String difficulty) {
        return String.format(
                "Sei un generatore di quiz educativi. Rispondi SOLO con JSON valido, senza testo aggiuntivo. " +
                        "Genera %d domande a scelta multipla su '%s' con difficoltà %s. " +
                        "Formato JSON richiesto: [{\"question\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correct\": \"A\"}]" +
                        "IMPORTANTE: Il campo 'correct' deve contenere SOLO la lettera della risposta corretta (A, B, C o D), non il testo. " +
                        "Rispondi SOLO con l'array JSON, nient'altro.",
                numQuestions, topic, difficulty
        );
    }

    /**
     * Costruisce il prompt per flashcards
     */
    private String buildFlashcardsPrompt(String topic, int numCards, DifficultyLevel difficulty) {
        return String.format(
                "Sei un generatore di flashcards educative. Rispondi SOLO con JSON valido, senza testo aggiuntivo. " +
                        "Genera %d flashcards su '%s' con difficoltà %s. " +
                        "Formato JSON richiesto: [{\"front\": \"domanda o concetto\", \"back\": \"risposta o spiegazione\"}]" +
                        "Le flashcards devono essere chiare, concise e utili per il ripasso. " +
                        "Rispondi SOLO con l'array JSON, nient'altro.",
                numCards, topic, difficulty.getLevel()
        );
    }

    /**
     * Costruisce il prompt per flashcards con contesto
     */
    private String buildFlashcardsWithContextPrompt(String topic, int numCards,
                                                    DifficultyLevel difficulty, String context) {
        return String.format(
                "Sei un generatore di flashcards educative. Rispondi SOLO con JSON valido, senza testo aggiuntivo. " +
                        "Genera %d flashcards su '%s' con difficoltà %s. " +
                        "Contesto aggiuntivo: %s. " +
                        "Formato JSON richiesto: [{\"front\": \"domanda o concetto\", \"back\": \"risposta o spiegazione\"}]" +
                        "Le flashcards devono essere chiare, concise e utili per il ripasso. " +
                        "Rispondi SOLO con l'array JSON, nient'altro.",
                numCards, topic, difficulty.getLevel(),
                context != null ? context : "nessuno"
        );
    }

    // ========================================
    // METODI PRIVATI - UTILITÀ
    // ========================================

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
     * Gestisce le eccezioni WebClient
     */
    private void handleWebClientException(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        log.error("Errore API Groq - Status: {}, Body: {}",
                statusCode, e.getResponseBodyAsString());

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
