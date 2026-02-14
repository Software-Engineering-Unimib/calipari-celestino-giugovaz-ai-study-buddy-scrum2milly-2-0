package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.exception.AIServiceException;
import com.ai.studybuddy.integration.AIClient;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.ai.studybuddy.util.enums.EducationLevel;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AIServiceImpl - Coverage Completa")
class AIServiceImplTest {

    @Mock
    private AIClient primaryClient;

    @Mock
    private AIClient fallbackClient;

    @InjectMocks
    private AIServiceImpl aiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "testFallback", false);
        
        // CONFIGURA I MOCK DI DEFAULT PER TUTTI I TEST
        when(primaryClient.isAvailable()).thenReturn(true);
        when(primaryClient.getModelName()).thenReturn("llama-3.1-70b");
        when(primaryClient.generateText(anyString())).thenReturn("Mock response");
        
        when(fallbackClient.isAvailable()).thenReturn(true);
        when(fallbackClient.getModelName()).thenReturn("llama-3.1-8b");
        when(fallbackClient.generateText(anyString())).thenReturn("Fallback response");
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON valido")
    void testParseFlashcardsResponse_ValidJson() {
        String jsonResponse = """
            [
                {"front": "Domanda 1", "back": "Risposta 1"},
                {"front": "Domanda 2", "back": "Risposta 2"}
            ]
            """;

        JsonArray result = aiService.parseFlashcardsResponse(jsonResponse);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON con markdown")
    void testParseFlashcardsResponse_WithMarkdown() {
        String jsonWithMarkdown = "```json\n[{\"front\":\"Test\",\"back\":\"Test\"}]\n```";
        JsonArray result = aiService.parseFlashcardsResponse(jsonWithMarkdown);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON invalido")
    void testParseFlashcardsResponse_InvalidJson() {
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse("This is not valid JSON");
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - null")
    void testParseFlashcardsResponse_NullResponse() {
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse(null);
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - stringa vuota")
    void testParseFlashcardsResponse_EmptyResponse() {
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse("");
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - solo spazi")
    void testParseFlashcardsResponse_OnlySpaces() {
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse("     ");
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - array vuoto")
    void testParseFlashcardsResponse_EmptyArray() {
        JsonArray result = aiService.parseFlashcardsResponse("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - non array")
    void testParseFlashcardsResponse_NotAnArray() {
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse("{\"front\":\"Test\",\"back\":\"Test\"}");
        });
    }

    @Test
    @DisplayName("generateExplanation - successo")
    void testGenerateExplanation_Success() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("generateExplanation - fallback")
    void testGenerateExplanation_FallbackOnPrimaryError() {
        when(primaryClient.generateText(anyString())).thenThrow(new RuntimeException("Primary failed"));
        
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        
        assertNotNull(result);
        assertEquals("Fallback response", result);
    }

    @Test
    @DisplayName("generateQuiz - String difficulty")
    void testGenerateQuiz_StringDifficulty_Success() {
        String result = aiService.generateQuiz("Fotosintesi", 5, "facile", EducationLevel.UNIVERSITY, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("generateQuiz - DifficultyLevel")
    void testGenerateQuiz_DifficultyLevel_Success() {
        String result = aiService.generateQuiz("Fotosintesi", 5, DifficultyLevel.INTERMEDIO, EducationLevel.UNIVERSITY, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("generateFlashcards - successo")
    void testGenerateFlashcards_Success() {
        String result = aiService.generateFlashcards("Fotosintesi", 3, DifficultyLevel.INTERMEDIO, EducationLevel.UNIVERSITY, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("generateFlashcardsWithContext - successo")
    void testGenerateFlashcardsWithContext_Success() {
        String result = aiService.generateFlashcardsWithContext("Fotosintesi", 3, DifficultyLevel.INTERMEDIO, "context", "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("generateFlashcardsWithContext - null context")
    void testGenerateFlashcardsWithContext_NullContext() {
        String result = aiService.generateFlashcardsWithContext("Fotosintesi", 3, DifficultyLevel.INTERMEDIO, null, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("generateQuiz deprecato - lancia UnsupportedOperationException")
    void testGenerateQuiz_Deprecated_NoLanguage() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateQuiz("Fotosintesi", 5, "facile");
        });
    }

    @Test
    @DisplayName("generateQuiz deprecato DifficultyLevel - lancia UnsupportedOperationException")
    void testGenerateQuiz_Deprecated_DifficultyNoLanguage() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateQuiz("Fotosintesi", 5, DifficultyLevel.INTERMEDIO);
        });
    }

    @Test
    @DisplayName("generateFlashCard deprecato - lancia UnsupportedOperationException")
    void testGenerateFlashCard_Deprecated() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateFlashCard("Fotosintesi", 3, "facile");
        });
    }

    @Test
    @DisplayName("generateFlashcards deprecato - lancia UnsupportedOperationException")
    void testGenerateFlashcards_Deprecated_NoLanguage() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateFlashcards("Fotosintesi", 3, DifficultyLevel.INTERMEDIO);
        });
    }

    @Test
    @DisplayName("generateFlashcardsWithContext deprecato - lancia UnsupportedOperationException")
    void testGenerateFlashcardsWithContext_Deprecated() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateFlashcardsWithContext("Fotosintesi", 3, DifficultyLevel.INTERMEDIO, "context");
        });
    }

    @Test
    @DisplayName("getAvailableModel - primary disponibile")
    void testGetAvailableModel_PrimaryAvailable() {
        String result = aiService.getAvailableModel();
        assertEquals("llama-3.1-70b", result);
    }

    @Test
    @DisplayName("getAvailableModel - solo fallback")
    void testGetAvailableModel_OnlyFallbackAvailable() {
        when(primaryClient.isAvailable()).thenReturn(false);
        
        String result = aiService.getAvailableModel();
        assertEquals("llama-3.1-8b", result);
    }

    @Test
    @DisplayName("getAvailableModel - nessuno disponibile")
    void testGetAvailableModel_NoneAvailable() {
        when(primaryClient.isAvailable()).thenReturn(false);
        when(fallbackClient.isAvailable()).thenReturn(false);
        
        String result = aiService.getAvailableModel();
        assertEquals("Nessun modello AI disponibile", result);
    }

    @Test
    @DisplayName("isAnyModelAvailable - primary")
    void testIsAnyModelAvailable_PrimaryAvailable() {
        assertTrue(aiService.isAnyModelAvailable());
    }

    @Test
    @DisplayName("isAnyModelAvailable - fallback")
    void testIsAnyModelAvailable_FallbackAvailable() {
        when(primaryClient.isAvailable()).thenReturn(false);
        assertTrue(aiService.isAnyModelAvailable());
    }

    @Test
    @DisplayName("isAnyModelAvailable - nessuno")
    void testIsAnyModelAvailable_NoneAvailable() {
        when(primaryClient.isAvailable()).thenReturn(false);
        when(fallbackClient.isAvailable()).thenReturn(false);
        assertFalse(aiService.isAnyModelAvailable());
    }

    @Test
    @DisplayName("Fallback - timeout")
    void testFallback_TimeoutError() {
        when(primaryClient.generateText(anyString())).thenThrow(new RuntimeException("Primary failed"));
        when(fallbackClient.generateText(anyString())).thenThrow(new RuntimeException("timeout occurred"));

        AIServiceException exception = assertThrows(AIServiceException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        });

        assertEquals(AIServiceException.AIErrorType.TIMEOUT, exception.getErrorType());
    }

    @Test
    @DisplayName("Fallback - rate limit 429")
    void testFallback_RateLimitError() {
        when(primaryClient.generateText(anyString())).thenThrow(new RuntimeException("Primary failed"));
        when(fallbackClient.generateText(anyString())).thenThrow(
            WebClientResponseException.create(429, "Too Many Requests", null, null, null)
        );

        AIServiceException exception = assertThrows(AIServiceException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        });

        assertEquals(AIServiceException.AIErrorType.RATE_LIMIT, exception.getErrorType());
    }

    @Test
    @DisplayName("Fallback - 401 invalid API key")
    void testFallback_InvalidApiKeyError() {
        when(primaryClient.generateText(anyString())).thenThrow(new RuntimeException("Primary failed"));
        when(fallbackClient.generateText(anyString())).thenThrow(
            WebClientResponseException.create(401, "Unauthorized", null, null, null)
        );

        AIServiceException exception = assertThrows(AIServiceException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        });

        assertEquals(AIServiceException.AIErrorType.INVALID_API_KEY, exception.getErrorType());
    }

    @Test
    @DisplayName("Fallback - 503")
    void testFallback_ServiceUnavailableError_503() {
        when(primaryClient.generateText(anyString())).thenThrow(new RuntimeException("Primary failed"));
        when(fallbackClient.generateText(anyString())).thenThrow(
            WebClientResponseException.create(503, "Service Unavailable", null, null, null)
        );

        AIServiceException exception = assertThrows(AIServiceException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        });

        assertEquals(AIServiceException.AIErrorType.SERVICE_UNAVAILABLE, exception.getErrorType());
    }

    @Test
    @DisplayName("Fallback - 502")
    void testFallback_ServiceUnavailableError_502() {
        when(primaryClient.generateText(anyString())).thenThrow(new RuntimeException("Primary failed"));
        when(fallbackClient.generateText(anyString())).thenThrow(
            WebClientResponseException.create(502, "Bad Gateway", null, null, null)
        );

        AIServiceException exception = assertThrows(AIServiceException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        });

        assertEquals(AIServiceException.AIErrorType.SERVICE_UNAVAILABLE, exception.getErrorType());
    }

    @Test
    @DisplayName("Fallback - 504")
    void testFallback_ServiceUnavailableError_504() {
        when(primaryClient.generateText(anyString())).thenThrow(new RuntimeException("Primary failed"));
        when(fallbackClient.generateText(anyString())).thenThrow(
            WebClientResponseException.create(504, "Gateway Timeout", null, null, null)
        );

        AIServiceException exception = assertThrows(AIServiceException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        });

        assertEquals(AIServiceException.AIErrorType.SERVICE_UNAVAILABLE, exception.getErrorType());
    }

    @Test
    @DisplayName("Fallback - errore generico WebClient")
    void testFallback_GenericWebClientError() {
        when(primaryClient.generateText(anyString())).thenThrow(new RuntimeException("Primary failed"));
        when(fallbackClient.generateText(anyString())).thenThrow(
            WebClientResponseException.create(500, "Internal Server Error", null, null, null)
        );

        AIServiceException exception = assertThrows(AIServiceException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        });

        assertEquals(AIServiceException.AIErrorType.SERVICE_UNAVAILABLE, exception.getErrorType());
    }

    @Test
    @DisplayName("Fallback - errore generico")
    void testFallback_GenericNonTimeoutError() {
        when(primaryClient.generateText(anyString())).thenThrow(new RuntimeException("Primary failed"));
        when(fallbackClient.generateText(anyString())).thenThrow(new RuntimeException("Generic error"));

        AIServiceException exception = assertThrows(AIServiceException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        });

        assertEquals(AIServiceException.AIErrorType.SERVICE_UNAVAILABLE, exception.getErrorType());
    }

    @Test
    @DisplayName("Test mode")
    void testTestMode_ForcesFallback() {
        ReflectionTestUtils.setField(aiService, "testFallback", true);

        AIServiceException exception = assertThrows(AIServiceException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        });

        assertEquals(AIServiceException.AIErrorType.RATE_LIMIT, exception.getErrorType());
    }

    @Test
    @DisplayName("EducationLevel - UNIVERSITY")
    void testEducationLevel_University() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("EducationLevel - HIGH_SCHOOL")
    void testEducationLevel_HighSchool() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.HIGH_SCHOOL, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("EducationLevel - MIDDLE_SCHOOL")
    void testEducationLevel_MiddleSchool() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.MIDDLE_SCHOOL, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("EducationLevel - ALTRO")
    void testEducationLevel_Altro() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.ALTRO, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Lingua - it")
    void testLanguage_Italian() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Lingua - en")
    void testLanguage_English() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "en");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Lingua - es")
    void testLanguage_Spanish() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "es");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Lingua - fr")
    void testLanguage_French() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "fr");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Lingua - de")
    void testLanguage_German() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "de");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Lingua - pt")
    void testLanguage_Portuguese() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "pt");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Lingua - ru")
    void testLanguage_Russian() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "ru");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Lingua - xx (non supportata)")
    void testLanguage_Unsupported() {
        String result = aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, "xx");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Lingua - null")
    void testLanguage_Null() {
        assertThrows(IllegalArgumentException.class, () -> {
            aiService.generateExplanation("Fotosintesi", EducationLevel.UNIVERSITY, null);
        });
    }

    @Test
    @DisplayName("DifficultyLevel - PRINCIPIANTE")
    void testDifficultyLevel_Principiante() {
        String result = aiService.generateFlashcards("Fotosintesi", 3, DifficultyLevel.PRINCIPIANTE, EducationLevel.UNIVERSITY, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("DifficultyLevel - INTERMEDIO")
    void testDifficultyLevel_Intermedio() {
        String result = aiService.generateFlashcards("Fotosintesi", 3, DifficultyLevel.INTERMEDIO, EducationLevel.UNIVERSITY, "it");
        assertNotNull(result);
    }

    @Test
    @DisplayName("DifficultyLevel - AVANZATO")
    void testDifficultyLevel_Avanzato() {
        String result = aiService.generateFlashcards("Fotosintesi", 3, DifficultyLevel.AVANZATO, EducationLevel.UNIVERSITY, "it");
        assertNotNull(result);
    }
}