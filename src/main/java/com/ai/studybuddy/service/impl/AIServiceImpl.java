package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.integration.AIClient;
import com.ai.studybuddy.exception.AIServiceException;
import com.ai.studybuddy.exception.AIServiceException.AIErrorType;
import com.ai.studybuddy.service.inter.AIService;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.ai.studybuddy.util.enums.EducationLevel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Servizio principale per la generazione di contenuti AI.
 * 
 * ⚠️ La lingua è SEMPRE obbligatoria e deve arrivare dal profilo utente.
 *    Nessun default fisso! I metodi legacy lanciano eccezione.
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
    // METODI CON LINGUA (OBBLIGATORIA)
    // ========================================

    @Override
    public String generateExplanation(String topic, EducationLevel educationLevel, String language) {
        log.info("Generazione spiegazione - topic: '{}', livello: {}, lingua: {}",
                topic, educationLevel, language);

        String prompt = buildExplanationPrompt(topic, educationLevel, language);
        return callAIWithFallback(prompt);
    }

    @Override
    public String generateQuiz(String topic, int numQuestions, String difficulty, EducationLevel educationLevel, String language) {
        log.info("Generazione quiz - topic: '{}', domande: {}, difficoltà: {}, lingua: {}",
                topic, numQuestions, difficulty, language);

        String prompt = buildQuizPrompt(topic, numQuestions, difficulty, educationLevel, language);
        return callAIWithFallback(prompt);
    }

    @Override
    public String generateQuiz(String topic, int numQuestions, DifficultyLevel difficulty, EducationLevel educationLevel, String language) {
        log.info("Generazione quiz - topic: '{}', domande: {}, difficoltà: {}, livello: {}, lingua: {}",
                topic, numQuestions, difficulty, educationLevel, language);

        String prompt = buildQuizPrompt(topic, numQuestions, difficulty.getLevel(), educationLevel, language);
        return callAIWithFallback(prompt);
    }


    @Override
    public String generateFlashcards(String topic, int numCards, DifficultyLevel difficulty, EducationLevel educationLevel, String language) {
        log.info("Generazione flashcards - topic: '{}', carte: {}, difficoltà: {}, livello: {}, lingua: {}",
                topic, numCards, difficulty, educationLevel, language);

        String prompt = buildFlashcardsPrompt(topic, numCards, difficulty, educationLevel, language);
        return callAIWithFallback(prompt);
    }

    @Override
    public String generateFlashcardsWithContext(String topic, int numCards,
                                                DifficultyLevel difficulty, String context, String language) {
        log.info("Generazione flashcards con contesto - topic: '{}', carte: {}, lingua: {}",
                topic, numCards, language);

        String prompt = buildFlashcardsWithContextPrompt(topic, numCards, difficulty, context, language);
        return callAIWithFallback(prompt);
    }

    // ========================================
    // METODI LEGACY (DEPRECATI - NON USARE)
    // ========================================



    @Override
    @Deprecated
    public String generateQuiz(String topic, int numQuestions, String difficulty) {
        throw new UnsupportedOperationException(
            "Metodo deprecato: la lingua è obbligatoria. Usa generateQuiz(topic, numQuestions, difficulty, language)"
        );
    }

    @Override
    @Deprecated
    public String generateQuiz(String topic, int numQuestions, DifficultyLevel difficulty) {
        throw new UnsupportedOperationException(
            "Metodo deprecato: la lingua è obbligatoria. Usa generateQuiz(topic, numQuestions, difficulty, language)"
        );
    }

    @Override
    @Deprecated
    public String generateFlashCard(String topic, int numCards, String difficulty) {
        throw new UnsupportedOperationException(
            "Metodo deprecato: usa generateFlashcards(topic, numCards, difficulty, language)"
        );
    }

    @Override
    @Deprecated
    public String generateFlashcards(String topic, int numCards, DifficultyLevel difficulty) {
        throw new UnsupportedOperationException(
            "Metodo deprecato: la lingua è obbligatoria. Usa generateFlashcards(topic, numCards, difficulty, language)"
        );
    }

    @Override
    @Deprecated
    public String generateFlashcardsWithContext(String topic, int numCards,
                                                DifficultyLevel difficulty, String context) {
        throw new UnsupportedOperationException(
            "Metodo deprecato: la lingua è obbligatoria. Usa generateFlashcardsWithContext(topic, numCards, difficulty, context, language)"
        );
    }

    // ========================================
    // PARSING RISPOSTE
    // ========================================

    @Override
    public JsonArray parseFlashcardsResponse(String aiResponse) {
        try {
            if (aiResponse == null || aiResponse.isEmpty()) {
                throw new AIServiceException(AIErrorType.RESPONSE_NULL);
            }
            
            String cleaned = cleanJsonResponse(aiResponse);
            return gson.fromJson(cleaned, JsonArray.class);
        } catch (JsonSyntaxException e) {
            log.error("Errore parsing JSON: {}", aiResponse);
            throw new AIServiceException(AIErrorType.PARSE_ERROR,
                    "Impossibile interpretare la risposta dell'AI");
        }
    }

    // ========================================
    // METODI UTILITÀ
    // ========================================

    @Override
    public String getAvailableModel() {
        if (primaryClient.isAvailable()) {
            return primaryClient.getModelName();
        }
        if (fallbackClient.isAvailable()) {
            return fallbackClient.getModelName();
        }
        return "Nessun modello AI disponibile";
    }

    @Override
    public boolean isAnyModelAvailable() {
        return primaryClient.isAvailable() || fallbackClient.isAvailable();
    }

    // ========================================
    // FALLBACK LOGIC
    // ========================================

    private String callAIWithFallback(String prompt) {
        if (testFallback) {
            log.warn("⚠️ TEST MODE ATTIVO: Forzando fallback al modello secondario");
            throw new AIServiceException(AIErrorType.RATE_LIMIT, "Test fallback");
        }

        try {
            log.debug("Tentativo con {}", primaryClient.getModelName());
            return primaryClient.generateText(prompt);
        } catch (Exception primaryError) {
            log.warn("Primary model fallito: {}", primaryError.getMessage());

            try {
                log.info("🔄 Fallback a {}", fallbackClient.getModelName());
                return fallbackClient.generateText(prompt);
            } catch (WebClientResponseException e) {
                handleWebClientException(e);
                throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE);
            } catch (Exception fallbackError) {
                log.error("❌ Anche il fallback model è fallito: {}", fallbackError.getMessage());

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
    // COSTRUZIONE PROMPT RAFFORZATI (LINGUA OBBLIGATORIA + LIVELLO)
    // ========================================

    /**
     * Costruisce il prompt per spiegazioni con istruzioni linguistiche vincolanti.
     */
    private String buildExplanationPrompt(String topic, EducationLevel educationLevel, String language) {
        String languageInstruction = getLanguageInstruction(language);
        
        return String.format(
                "%s\n\n" +
                "⚠️ È ASSOLUTAMENTE OBBLIGATORIO rispondere ESCLUSIVAMENTE nella lingua specificata.\n" +
                "⚠️ QUALSIASI PAROLA, FRASE O SPIEGAZIONE IN ITALIANO O IN ALTRA LINGUA È VIETATA E SARÀ CONSIDERATA ERRORE.\n\n" +
                "Sei un tutor paziente e chiaro.\n" +
                "Spiega '%s' a uno studente di livello %s.\n" +
                "Usa esempi concreti e un linguaggio appropriato al livello.\n" +
                "Parla SOLO nella lingua specificata e NON COMMENTARE la richiesta.\n" +
                "Adatta il contenuto alla cultura e al sistema educativo della lingua target se rilevante.\n\n" +
                "La spiegazione deve essere chiara, ben strutturata e facile da capire.\n\n" +
                "RICORDA: TUTTA LA RISPOSTA DEVE ESSERE NELLA LINGUA: %s.",
                languageInstruction, topic, educationLevel, language
        );
    }

    /**
     * Costruisce il prompt per quiz con istruzioni linguistiche vincolanti e formato JSON.
     */
    private String buildQuizPrompt(String topic, int numQuestions, String difficulty, EducationLevel educationLevel, String language) {
        String languageInstruction = getLanguageInstruction(language);
        
        return String.format(
                "%s\n\n" +
                "⚠️ È ASSOLUTAMENTE OBBLIGATORIO rispondere ESCLUSIVAMENTE nella lingua specificata.\n" +
                "⚠️ TUTTO IL CONTENUTO (domande, opzioni, eventuali testi) DEVE ESSERE NELLA LINGUA: %s.\n" +
                "⚠️ NON AGGIUNGERE NESSUN TESTO FUORI DAL JSON, NEPPURE INTRODUZIONI O COMMENTI.\n\n" +
                "Sei un generatore di quiz educativi. Rispondi SOLO con JSON valido, senza testo aggiuntivo.\n" +
                "Genera %d domande a scelta multipla su '%s' con difficoltà %s.\n"+"Per uno studente di livello %s"+
                "Formato JSON richiesto: [{\"question\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correct\": \"A\"}]\n" +
                "IMPORTANTE: Il campo 'correct' deve contenere SOLO la lettera della risposta corretta (A, B, C o D), non il testo.\n" +
                "TUTTO il contenuto (domande e opzioni) deve essere ESCLUSIVAMENTE nella lingua specificata.\n" +
                "Rispondi SOLO con l'array JSON, nient'altro.",
                languageInstruction, language, numQuestions, topic, difficulty, educationLevel
        );
    }

    /**
     * Costruisce il prompt per flashcards con istruzioni linguistiche vincolanti e formato JSON.
     */
    private String buildFlashcardsPrompt(String topic, int numCards, DifficultyLevel difficulty, EducationLevel educationLevel, String language) {
        String languageInstruction = getLanguageInstruction(language);
        
        return String.format(
                "%s\n\n" +
                "⚠️ È ASSOLUTAMENTE OBBLIGATORIO rispondere ESCLUSIVAMENTE nella lingua specificata.\n" +
                "⚠️ TUTTO IL CONTENUTO (front, back) DEVE ESSERE NELLA LINGUA: %s.\n" +
                "⚠️ NON AGGIUNGERE NESSUN TESTO FUORI DAL JSON, NEPPURE INTRODUZIONI O COMMENTI.\n\n" +
                "Sei un generatore di flashcards educative. Rispondi SOLO con JSON valido, senza testo aggiuntivo.\n" +
                "Genera %d flashcards su '%s' con difficoltà %s.\n" +
                "Formato JSON richiesto: [{\"front\": \"domanda o concetto\", \"back\": \"risposta o spiegazione\"}]\n" +
                "Le flashcards devono essere chiare, concise e utili per il ripasso.\n" +
                "TUTTO il contenuto (front e back) deve essere ESCLUSIVAMENTE nella lingua specificata.\n" +
                "Rispondi SOLO con l'array JSON, nient'altro.",
                languageInstruction, language, numCards, topic, difficulty.getLevel()
        );
    }

    /**
     * Costruisce il prompt per flashcards con contesto, con istruzioni linguistiche vincolanti.
     */
    private String buildFlashcardsWithContextPrompt(String topic, int numCards,
                                                    DifficultyLevel difficulty, String context, String language) {
        String languageInstruction = getLanguageInstruction(language);
        
        return String.format(
                "%s\n\n" +
                "⚠️ È ASSOLUTAMENTE OBBLIGATORIO rispondere ESCLUSIVAMENTE nella lingua specificata.\n" +
                "⚠️ TUTTO IL CONTENUTO (front, back) DEVE ESSERE NELLA LINGUA: %s.\n" +
                "⚠️ NON AGGIUNGERE NESSUN TESTO FUORI DAL JSON, NEPPURE INTRODUZIONI O COMMENTI.\n\n" +
                "Sei un generatore di flashcards educative. Rispondi SOLO con JSON valido, senza testo aggiuntivo.\n" +
                "Genera %d flashcards su '%s' con difficoltà %s.\n" +
                "Contesto aggiuntivo: %s.\n\n" +
                "Formato JSON richiesto: [{\"front\": \"domanda o concetto\", \"back\": \"risposta o spiegazione\"}]\n" +
                "Le flashcards devono essere chiare, concise e utili per il ripasso.\n" +
                "TUTTO il contenuto (front e back) deve essere ESCLUSIVAMENTE nella lingua specificata.\n" +
                "Rispondi SOLO con l'array JSON, nient'altro.",
                languageInstruction, language, numCards, topic, difficulty.getLevel(),
                context != null ? context : "nessuno"
        );
    }

    /**
     * Restituisce l'istruzione di lingua per il prompt.
     * ⚠️ language NON può essere null!
     */
    private String getLanguageInstruction(String language) {
        if (language == null) {
            throw new IllegalArgumentException("La lingua non può essere null");
        }
        
        Map<String, String> languageInstructions = new HashMap<>();
        languageInstructions.put("it", "Rispondi SEMPRE in italiano.");
        languageInstructions.put("en", "Respond ALWAYS in English.");
        languageInstructions.put("es", "Responde SIEMPRE en español.");
        languageInstructions.put("fr", "Réponds TOUJOURS en français.");
        languageInstructions.put("de", "Antworte IMMER auf Deutsch.");
        languageInstructions.put("pt", "Responda SEMPRE em português.");
        languageInstructions.put("ru", "Отвечай ВСЕГДА на русском языке.");
      
        return languageInstructions.getOrDefault(language.toLowerCase(), 
                String.format("Rispondi SEMPRE in %s.", language));
    }

    // ========================================
    // UTILITÀ
    // ========================================

    private String cleanJsonResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new AIServiceException(AIErrorType.PARSE_ERROR, "Risposta AI vuota");
        }
        return response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    private void handleWebClientException(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        log.error("Errore API Groq - Status: {}, Body: {}",
                statusCode, e.getResponseBodyAsString());

        switch (statusCode) {
            case 429:
                throw new AIServiceException(AIErrorType.RATE_LIMIT);
            case 401:
                throw new AIServiceException(AIErrorType.INVALID_API_KEY);
            case 503: case 502: case 504:
                throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE);
            default:
                throw new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE,
                        "Errore API: " + e.getMessage());
        }
    }
}