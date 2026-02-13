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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AIServiceImpl - Test Suite Completo")
class AIServiceImplTest {

    @Mock
    private AIClient primaryClient;

    @Mock
    private AIClient fallbackClient;

    @InjectMocks
    private AIServiceImpl aiService;

    private static final String TEST_TOPIC = "Fotosintesi";
    private static final int TEST_NUM_CARDS = 3;
    private static final EducationLevel TEST_EDUCATION_LEVEL = EducationLevel.UNIVERSITY;
    private static final DifficultyLevel TEST_DIFFICULTY = DifficultyLevel.INTERMEDIO;

    @BeforeEach
    void setUp() {
        try {
            ReflectionTestUtils.setField(aiService, "testFallback", false);
        } catch (Exception e) {
            // Campo non esiste, ignora
        }
    }

    // ========================================
    // TEST: parseFlashcardsResponse SOLO
    // ========================================

    @Test
    @DisplayName("parseFlashcardsResponse - Parsing JSON valido")
    void testParseFlashcardsResponse_ValidJson() {
        // Arrange
        String jsonResponse = """
            [
                {"front": "Domanda 1", "back": "Risposta 1"},
                {"front": "Domanda 2", "back": "Risposta 2"}
            ]
            """;

        // Act
        JsonArray result = aiService.parseFlashcardsResponse(jsonResponse);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Domanda 1", result.get(0).getAsJsonObject().get("front").getAsString());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON con markdown backticks")
    void testParseFlashcardsResponse_WithMarkdown() {
        // Arrange
        String jsonWithMarkdown = "```json\n[{\"front\":\"Test\",\"back\":\"Test\"}]\n```";

        // Act
        JsonArray result = aiService.parseFlashcardsResponse(jsonWithMarkdown);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - Errore su JSON invalido")
    void testParseFlashcardsResponse_InvalidJson() {
        // Arrange
        String invalidJson = "This is not valid JSON";

        // Act & Assert
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse(invalidJson);
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - Errore su risposta null")
    void testParseFlashcardsResponse_NullResponse() {
        // Act & Assert
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse(null);
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - Errore su risposta vuota")
    void testParseFlashcardsResponse_EmptyResponse() {
        // Act & Assert
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse("");
        });
    }

    // ========================================
    // TEST: Metodi Deprecati
    // ========================================

    @Test
    @DisplayName("generateQuiz (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateQuiz_Deprecated_ThrowsException() {
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateQuiz(TEST_TOPIC, 5, "easy");
        });
    }

    @Test
    @DisplayName("generateQuiz DifficultyLevel (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateQuizDifficulty_Deprecated_ThrowsException() {
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateQuiz(TEST_TOPIC, 5, TEST_DIFFICULTY);
        });
    }

    @Test
    @DisplayName("generateFlashCard (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateFlashCard_Deprecated_ThrowsException() {
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateFlashCard(TEST_TOPIC, TEST_NUM_CARDS, "medium");
        });
    }

    @Test
    @DisplayName("generateFlashcards (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateFlashcards_Deprecated_ThrowsException() {
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateFlashcards(TEST_TOPIC, TEST_NUM_CARDS, TEST_DIFFICULTY);
        });
    }

    @Test
    @DisplayName("generateFlashcardsWithContext (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateFlashcardsWithContext_Deprecated_ThrowsException() {
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateFlashcardsWithContext(TEST_TOPIC, TEST_NUM_CARDS, TEST_DIFFICULTY, "context");
        });
    }

    // ========================================
    // TEST: Validazione
    // ========================================

    @Test
    @DisplayName("Validazione Lingua - Lingua null lancia eccezione")
    void testLanguageValidation_NullLanguage() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            aiService.generateExplanation(TEST_TOPIC, TEST_EDUCATION_LEVEL, null);
        });
    }
}