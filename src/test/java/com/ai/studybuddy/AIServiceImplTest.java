package com.ai.studybuddy;

import com.ai.studybuddy.service.impl.AIServiceImpl;
import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.config.integration.AIClient;
import com.ai.studybuddy.exception.AIServiceException;
import com.ai.studybuddy.exception.AIServiceException.AIErrorType;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test completi per AIServiceImpl con fallback tra Primary e Fallback AI.
 *
 * Copre:
 * - Successo Primary AI
 * - Fallback quando Primary fallisce
 * - Errore quando entrambi falliscono
 * - Parsing JSON
 * - Disponibilità modelli
 * - Test mode
 *
 *
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AIServiceImpl - Test Primary e Fallback AI")
class AIServiceImplTest {

    @Mock
    private AIClient primaryClient;

    @Mock
    private AIClient fallbackClient;

    private AIServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AIServiceImpl(primaryClient, fallbackClient);

        // Disabilita test mode per default
        ReflectionTestUtils.setField(aiService, "testFallback", false);


    }

    // ==========================================
    // TEST PRIMARY AI - SUCCESS
    // ==========================================

    @Test
    @DisplayName("✅ PRIMARY: Genera spiegazione con successo")
    void generateExplanation_PrimarySuccess() {
        // Given
        String expectedResponse = "La fotosintesi è il processo con cui le piante producono energia dalla luce solare...";
        when(primaryClient.generateText(anyString())).thenReturn(expectedResponse);

        // When
        String result = aiService.generateExplanation("fotosintesi", "intermedio");

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(primaryClient, times(1)).generateText(anyString());
        verify(fallbackClient, never()).generateText(anyString());
    }

    @Test
    @DisplayName("✅ PRIMARY: Genera quiz con successo")
    void generateQuiz_PrimarySuccess() {
        // Given
        String expectedJson = "[{\"question\":\"Cos'è la fotosintesi?\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correct\":\"A\"}]";
        when(primaryClient.generateText(anyString())).thenReturn(expectedJson);

        // When
        String result = aiService.generateQuiz("biologia", 5, "medio");

        // Then
        assertThat(result).isEqualTo(expectedJson);
        verify(primaryClient, times(1)).generateText(anyString());
        verify(fallbackClient, never()).generateText(anyString());
    }

    @Test
    @DisplayName("✅ PRIMARY: Genera flashcards con successo")
    void generateFlashcards_PrimarySuccess() {
        // Given
        String expectedJson = "[{\"front\":\"Cos'è il DNA?\",\"back\":\"Acido desossiribonucleico\"}]";
        when(primaryClient.generateText(anyString())).thenReturn(expectedJson);

        // When
        String result = aiService.generateFlashcards("genetica", 10, DifficultyLevel.AVANZATO);

        // Then
        assertThat(result).isEqualTo(expectedJson);
        verify(primaryClient, times(1)).generateText(anyString());
        verify(fallbackClient, never()).generateText(anyString());
    }

    // ==========================================
    // TEST FALLBACK AI - PRIMARY FAILS
    // ==========================================

    @Test
    @DisplayName("🔄 FALLBACK: Attivato quando PRIMARY fallisce per RATE_LIMIT")
    void generateExplanation_FallbackOnRateLimit() {
        // Given
        String fallbackResponse = "Risposta dal modello di fallback...";

        when(primaryClient.generateText(anyString()))
                .thenThrow(new AIServiceException(AIErrorType.RATE_LIMIT));
        when(fallbackClient.generateText(anyString()))
                .thenReturn(fallbackResponse);

        // When
        String result = aiService.generateExplanation("matematica", "avanzato");

        // Then
        assertThat(result).isEqualTo(fallbackResponse);
        verify(primaryClient, times(1)).generateText(anyString());
        verify(fallbackClient, times(1)).generateText(anyString());
    }

    @Test
    @DisplayName("🔄 FALLBACK: Attivato quando PRIMARY fallisce per TIMEOUT")
    void generateQuiz_FallbackOnTimeout() {
        // Given
        String fallbackJson = "[{\"question\":\"Test timeout\",\"options\":[\"A\",\"B\"],\"correct\":\"A\"}]";

        when(primaryClient.generateText(anyString()))
                .thenThrow(new AIServiceException(AIErrorType.TIMEOUT));
        when(fallbackClient.generateText(anyString()))
                .thenReturn(fallbackJson);

        // When
        String result = aiService.generateQuiz("fisica", 3, "facile");

        // Then
        assertThat(result).isEqualTo(fallbackJson);
        verify(primaryClient, times(1)).generateText(anyString());
        verify(fallbackClient, times(1)).generateText(anyString());
    }

    @Test
    @DisplayName("🔄 FALLBACK: Attivato quando PRIMARY fallisce per SERVICE_UNAVAILABLE")
    void generateFlashcards_FallbackOnServiceUnavailable() {
        // Given
        String fallbackJson = "[{\"front\":\"Domanda\",\"back\":\"Risposta\"}]";

        when(primaryClient.generateText(anyString()))
                .thenThrow(new AIServiceException(AIErrorType.SERVICE_UNAVAILABLE));
        when(fallbackClient.generateText(anyString()))
                .thenReturn(fallbackJson);

        // When
        String result = aiService.generateFlashcards("chimica", 5, DifficultyLevel.INTERMEDIO);

        // Then
        assertThat(result).isEqualTo(fallbackJson);
        verify(primaryClient, times(1)).generateText(anyString());
        verify(fallbackClient, times(1)).generateText(anyString());
    }

    @Test
    @DisplayName("🔄 FALLBACK: Attivato quando PRIMARY lancia RuntimeException generico")
    void generateExplanation_FallbackOnGenericException() {
        // Given
        String fallbackResponse = "Fallback attivo";

        when(primaryClient.generateText(anyString()))
                .thenThrow(new RuntimeException("Errore generico primary"));
        when(fallbackClient.generateText(anyString()))
                .thenReturn(fallbackResponse);

        // When
        String result = aiService.generateExplanation("test", "base");

        // Then
        assertThat(result).isEqualTo(fallbackResponse);
        verify(fallbackClient, times(1)).generateText(anyString());
    }

    // ==========================================
    // TEST FAILURE - BOTH MODELS FAIL
    // ==========================================

    @Test
    @DisplayName("❌ ERRORE: Entrambi i modelli falliscono per RATE_LIMIT")
    void generateExplanation_BothFailWithRateLimit() {
        // Given
        when(primaryClient.generateText(anyString()))
                .thenThrow(new AIServiceException(AIErrorType.RATE_LIMIT));
        when(fallbackClient.generateText(anyString()))
                .thenThrow(new AIServiceException(AIErrorType.RATE_LIMIT));

        // When & Then
        assertThatThrownBy(() -> aiService.generateExplanation("test", "base"))
                .isInstanceOf(AIServiceException.class)
                .hasFieldOrPropertyWithValue("errorType", AIErrorType.SERVICE_UNAVAILABLE)
                .hasMessageContaining("Tutti i modelli AI non disponibili");

        verify(primaryClient, times(1)).generateText(anyString());
        verify(fallbackClient, times(1)).generateText(anyString());
    }

    @Test
    @DisplayName("❌ ERRORE: Primary TIMEOUT, Fallback INVALID_API_KEY")
    void generateQuiz_BothFailWithDifferentErrors() {
        // Given
        when(primaryClient.generateText(anyString()))
                .thenThrow(new AIServiceException(AIErrorType.TIMEOUT));
        when(fallbackClient.generateText(anyString()))
                .thenThrow(new AIServiceException(AIErrorType.INVALID_API_KEY));

        // When & Then
        assertThatThrownBy(() -> aiService.generateQuiz("geografia", 10, "difficile"))
                .isInstanceOf(AIServiceException.class)
                .hasFieldOrPropertyWithValue("errorType", AIErrorType.SERVICE_UNAVAILABLE);

        verify(primaryClient, times(1)).generateText(anyString());
        verify(fallbackClient, times(1)).generateText(anyString());
    }

    // ==========================================
    // TEST PARSING JSON
    // ==========================================

    @Test
    @DisplayName("📄 PARSING: JSON valido parsato correttamente")
    void parseFlashcardsResponse_ValidJson() {
        // Given
        String validJson = "[{\"front\":\"Q1\",\"back\":\"A1\"},{\"front\":\"Q2\",\"back\":\"A2\"}]";

        // When
        JsonArray result = aiService.parseFlashcardsResponse(validJson);

        // Then
        assertThat(result)
                .isNotNull()
                .hasSize(2);
    }

    @Test
    @DisplayName("📄 PARSING: JSON con markdown backticks rimossi")
    void parseFlashcardsResponse_WithMarkdown() {
        // Given
        String jsonWithMarkdown = "```json\n[{\"front\":\"Test\",\"back\":\"Answer\"}]\n```";

        // When
        JsonArray result = aiService.parseFlashcardsResponse(jsonWithMarkdown);

        // Then
        assertThat(result)
                .isNotNull()
                .hasSize(1);
    }

    @Test
    @DisplayName("📄 PARSING: JSON non valido lancia PARSE_ERROR")
    void parseFlashcardsResponse_InvalidJson() {
        // Given
        String invalidJson = "Questo non è JSON";

        // When & Then
        assertThatThrownBy(() -> aiService.parseFlashcardsResponse(invalidJson))
                .isInstanceOf(AIServiceException.class)
                .hasFieldOrPropertyWithValue("errorType", AIErrorType.PARSE_ERROR);
    }

    @Test
    @DisplayName("📄 PARSING: Risposta vuota lancia PARSE_ERROR")
    void parseFlashcardsResponse_EmptyResponse() {
        // When & Then
        assertThatThrownBy(() -> aiService.parseFlashcardsResponse(""))
                .isInstanceOf(AIServiceException.class)
                .hasFieldOrPropertyWithValue("errorType", AIErrorType.RESPONSE_NULL)
                .hasMessageContaining("Risposta AI vuota");
    }

    @Test
    @DisplayName("📄 PARSING: Risposta null lancia PARSE_ERROR")
    void parseFlashcardsResponse_NullResponse() {
        // When & Then
        assertThatThrownBy(() -> aiService.parseFlashcardsResponse(null))
                .isInstanceOf(AIServiceException.class)
                .hasFieldOrPropertyWithValue("errorType", AIErrorType.RESPONSE_NULL);
    }

    // ==========================================
    // TEST AVAILABILITY
    // ==========================================

    @Test
    @DisplayName("🔍 AVAILABILITY: Restituisce PRIMARY quando disponibile")
    void getAvailableModel_PrimaryAvailable() {
        // Given
        when(primaryClient.isAvailable()).thenReturn(true);
        when(primaryClient.getModelName()).thenReturn("Llama 3.3 (Primary)");

        // When
        String result = aiService.getAvailableModel();

        // Then
        assertThat(result).contains("Primary");
        verify(primaryClient, times(1)).isAvailable();
        verify(fallbackClient, never()).isAvailable();
    }

    @Test
    @DisplayName("🔍 AVAILABILITY: Restituisce FALLBACK quando Primary non disponibile")
    void getAvailableModel_OnlyFallbackAvailable() {
        // Given
        when(primaryClient.isAvailable()).thenReturn(false);
        when(fallbackClient.isAvailable()).thenReturn(true);
        when(fallbackClient.getModelName()).thenReturn("Llama 3.3 (Fallback)");
        // When
        String result = aiService.getAvailableModel();

        // Then
        assertThat(result).contains("Fallback");
        verify(primaryClient, times(1)).isAvailable();
        verify(fallbackClient, times(1)).isAvailable();
    }

    @Test
    @DisplayName("🔍 AVAILABILITY: Nessun modello disponibile")
    void getAvailableModel_NoneAvailable() {
        // Given
        when(primaryClient.isAvailable()).thenReturn(false);
        when(fallbackClient.isAvailable()).thenReturn(false);

        // When
        String result = aiService.getAvailableModel();

        // Then
        assertThat(result).isEqualTo("Nessun modello AI disponibile");
    }

    @Test
    @DisplayName("🔍 AVAILABILITY: isAnyModelAvailable = true quando Primary disponibile")
    void isAnyModelAvailable_PrimaryAvailable() {
        // Given
        when(primaryClient.isAvailable()).thenReturn(true);

        // When
        boolean result = aiService.isAnyModelAvailable();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("🔍 AVAILABILITY: isAnyModelAvailable = false quando nessuno disponibile")
    void isAnyModelAvailable_NoneAvailable() {
        // Given
        when(primaryClient.isAvailable()).thenReturn(false);
        when(fallbackClient.isAvailable()).thenReturn(false);

        // When
        boolean result = aiService.isAnyModelAvailable();

        // Then
        assertThat(result).isFalse();
    }

    // ==========================================
    // TEST MODE
    // ==========================================

    @Test
    @DisplayName("⚠️ TEST MODE: Forza fallback anche se Primary funziona")
    void testMode_ForcesFallback() {
        // Given
        ReflectionTestUtils.setField(aiService, "testFallback", true);
        // When & Then
        assertThatThrownBy(() -> aiService.generateExplanation("test", "base"))
                .isInstanceOf(AIServiceException.class)
                .hasFieldOrPropertyWithValue("errorType", AIErrorType.RATE_LIMIT)
                .hasMessageContaining("Test fallback");

        verify(primaryClient, never()).generateText(anyString());
    }

    // ==========================================
    // TEST ADDITIONAL METHODS
    // ==========================================

    @Test
    @DisplayName("🔢 Quiz con DifficultyLevel enum")
    void generateQuiz_WithDifficultyEnum() {
        // Given
        String expectedJson = "[{\"question\":\"Test\",\"options\":[\"A\",\"B\"],\"correct\":\"A\"}]";
        when(primaryClient.generateText(anyString())).thenReturn(expectedJson);

        // When
        String result = aiService.generateQuiz("informatica", 7, DifficultyLevel.PRINCIPIANTE);

        // Then
        assertThat(result).isEqualTo(expectedJson);
        verify(primaryClient, times(1)).generateText(anyString());
    }

    @Test
    @DisplayName("📚 Flashcards con contesto")
    void generateFlashcardsWithContext_Success() {
        // Given
        String expectedJson = "[{\"front\":\"Test\",\"back\":\"Answer\"}]";
        when(primaryClient.generateText(anyString())).thenReturn(expectedJson);

        // When
        String result = aiService.generateFlashcardsWithContext(
                "letteratura", 5, DifficultyLevel.INTERMEDIO, "Autori del '900"
        );

        // Then
        assertThat(result).isEqualTo(expectedJson);
        verify(primaryClient, times(1)).generateText(anyString());
    }

    @Test
    @DisplayName("📚 Flashcards con contesto null gestito correttamente")
    void generateFlashcardsWithContext_NullContext() {
        // Given
        String expectedJson = "[{\"front\":\"Test\",\"back\":\"Answer\"}]";
        when(primaryClient.generateText(anyString())).thenReturn(expectedJson);

        // When
        String result = aiService.generateFlashcardsWithContext(
                "arte", 3, DifficultyLevel.PRINCIPIANTE, null
        );

        // Then
        assertThat(result).isEqualTo(expectedJson);
        verify(primaryClient, times(1)).generateText(anyString());
    }

    @Test
    @DisplayName("⚠️ Metodo deprecato generateFlashCard delega a generateFlashcards")
    @SuppressWarnings("deprecation")
    void generateFlashCard_Deprecated() {
        // Given
        String expectedJson = "[{\"front\":\"Test\",\"back\":\"Answer\"}]";
        when(primaryClient.generateText(anyString())).thenReturn(expectedJson);

        // When
        String result = aiService.generateFlashCard("scienze", 4, "medio");

        // Then
        assertThat(result).isEqualTo(expectedJson);
        verify(primaryClient, times(1)).generateText(anyString());
    }
}