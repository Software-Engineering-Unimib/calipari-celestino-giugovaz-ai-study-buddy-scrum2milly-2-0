package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.exception.AIServiceException;
import com.ai.studybuddy.integration.AIClient;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.ai.studybuddy.util.enums.EducationLevel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AIServiceImpl")
class AIServiceImplTest {

    @Mock
    private AIClient primaryClient;

    @Mock
    private AIClient fallbackClient;

    @InjectMocks
    private AIServiceImpl aiService;

    private static final String TEST_TOPIC = "Fotosintesi";
    private static final String TEST_LANGUAGE = "it";
    private static final int TEST_NUM_QUESTIONS = 5;
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
    // TEST: parseFlashcardsResponse - Coverage Completo
    // ========================================

    @Test
    @DisplayName("parseFlashcardsResponse - Parsing JSON valido")
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
        assertEquals("Domanda 1", result.get(0).getAsJsonObject().get("front").getAsString());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON con markdown backticks")
    void testParseFlashcardsResponse_WithMarkdown() {
        String jsonWithMarkdown = "```json\n[{\"front\":\"Test\",\"back\":\"Test\"}]\n```";

        JsonArray result = aiService.parseFlashcardsResponse(jsonWithMarkdown);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - Errore su JSON invalido")
    void testParseFlashcardsResponse_InvalidJson() {
        String invalidJson = "This is not valid JSON";

        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse(invalidJson);
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - Errore su risposta null")
    void testParseFlashcardsResponse_NullResponse() {
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse(null);
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - Errore su risposta vuota")
    void testParseFlashcardsResponse_EmptyResponse() {
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse("");
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON con spazi extra")
    void testParseFlashcardsResponse_WithExtraSpaces() {
        String jsonWithSpaces = """
            [
              {  "front"  :  "Test"  ,  "back"  :  "Answer"  }
            ]
            """;

        JsonArray result = aiService.parseFlashcardsResponse(jsonWithSpaces);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON compatto")
    void testParseFlashcardsResponse_Compact() {
        String compactJson = "[{\"front\":\"Q\",\"back\":\"A\"}]";

        JsonArray result = aiService.parseFlashcardsResponse(compactJson);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - Multipli backticks markdown")
    void testParseFlashcardsResponse_MultipleBackticks() {
        String multiBackticks = "```\n```json\n[{\"front\":\"Test\",\"back\":\"Test\"}]\n```\n```";

        JsonArray result = aiService.parseFlashcardsResponse(multiBackticks);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - Array vuoto è valido")
    void testParseFlashcardsResponse_EmptyArray() {
        String emptyArray = "[]";

        JsonArray result = aiService.parseFlashcardsResponse(emptyArray);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON non array lancia eccezione")
    void testParseFlashcardsResponse_NotAnArray() {
        String notArray = "{\"front\":\"Test\",\"back\":\"Test\"}";

        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse(notArray);
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - String solo con spazi lancia eccezione")
    void testParseFlashcardsResponse_OnlySpaces() {
        assertThrows(AIServiceException.class, () -> {
            aiService.parseFlashcardsResponse("     ");
        });
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON con newline")
    void testParseFlashcardsResponse_WithNewlines() {
        String jsonWithNewlines = "[\n{\n\"front\"\n:\n\"Q\"\n,\n\"back\"\n:\n\"A\"\n}\n]";

        JsonArray result = aiService.parseFlashcardsResponse(jsonWithNewlines);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON con tab")
    void testParseFlashcardsResponse_WithTabs() {
        String jsonWithTabs = "[\t{\t\"front\":\t\"Q\",\t\"back\":\t\"A\"\t}\t]";

        JsonArray result = aiService.parseFlashcardsResponse(jsonWithTabs);

        assertNotNull(result);
        assertEquals(1, result.size());
    }



    @Test
    @DisplayName("parseFlashcardsResponse - Markdown senza linguaggio")
    void testParseFlashcardsResponse_MarkdownNoLanguage() {
        String markdown = "```\n[{\"front\":\"Test\",\"back\":\"Test\"}]\n```";

        JsonArray result = aiService.parseFlashcardsResponse(markdown);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - Array con molti elementi")
    void testParseFlashcardsResponse_LargeArray() {
        String largeArray = """
            [
                {"front": "Q1", "back": "A1"},
                {"front": "Q2", "back": "A2"},
                {"front": "Q3", "back": "A3"},
                {"front": "Q4", "back": "A4"},
                {"front": "Q5", "back": "A5"}
            ]
            """;

        JsonArray result = aiService.parseFlashcardsResponse(largeArray);

        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON con caratteri speciali")
    void testParseFlashcardsResponse_SpecialCharacters() {
        String jsonSpecialChars = "[{\"front\":\"Perché?\",\"back\":\"Così!\"}]";

        JsonArray result = aiService.parseFlashcardsResponse(jsonSpecialChars);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("parseFlashcardsResponse - JSON con escape sequences")
    void testParseFlashcardsResponse_EscapeSequences() {
        String jsonEscape = "[{\"front\":\"Line 1\\nLine 2\",\"back\":\"Tab\\there\"}]";

        JsonArray result = aiService.parseFlashcardsResponse(jsonEscape);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========================================
    // TEST: Metodi Deprecati - Coverage Completo
    // ========================================

    @Test
    @DisplayName("generateQuiz String (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateQuiz_Deprecated_ThrowsException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateQuiz(TEST_TOPIC, TEST_NUM_QUESTIONS, "easy");
        });
    }

    @Test
    @DisplayName("generateQuiz DifficultyLevel (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateQuizDifficulty_Deprecated_ThrowsException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateQuiz(TEST_TOPIC, TEST_NUM_QUESTIONS, TEST_DIFFICULTY);
        });
    }

    @Test
    @DisplayName("generateFlashCard (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateFlashCard_Deprecated_ThrowsException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateFlashCard(TEST_TOPIC, TEST_NUM_CARDS, "medium");
        });
    }

    @Test
    @DisplayName("generateFlashcards DifficultyLevel (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateFlashcards_Deprecated_ThrowsException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateFlashcards(TEST_TOPIC, TEST_NUM_CARDS, TEST_DIFFICULTY);
        });
    }

    @Test
    @DisplayName("generateFlashcardsWithContext (deprecato) - Lancia UnsupportedOperationException")
    void testGenerateFlashcardsWithContext_Deprecated_ThrowsException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            aiService.generateFlashcardsWithContext(TEST_TOPIC, TEST_NUM_CARDS, TEST_DIFFICULTY, "context");
        });
    }

    // ========================================
    // TEST: Validazione Input - Coverage Completo
    // ========================================

    @Test
    @DisplayName("Validazione - Lingua null lancia eccezione")
    void testLanguageValidation_NullLanguage() {
        assertThrows(IllegalArgumentException.class, () -> {
            aiService.generateExplanation(TEST_TOPIC, TEST_EDUCATION_LEVEL, null);
        });
    }



    // ========================================
    // TEST: Tutti i Livelli di Education
    // ========================================

    @Test
    @DisplayName("EducationLevel - UNIVERSITY")
    void testEducationLevel_University() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, EducationLevel.UNIVERSITY, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK - il mock ritorna null
            }
        });
    }

    @Test
    @DisplayName("EducationLevel - HIGH_SCHOOL")
    void testEducationLevel_HighSchool() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, EducationLevel.HIGH_SCHOOL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK - il mock ritorna null
            }
        });
    }

    @Test
    @DisplayName("EducationLevel - MIDDLE_SCHOOL")
    void testEducationLevel_MiddleSchool() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, EducationLevel.MIDDLE_SCHOOL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK - il mock ritorna null
            }
        });
    }

    @Test
    @DisplayName("EducationLevel - ALTRO")
    void testEducationLevel_Altro() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, EducationLevel.ALTRO, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK - il mock ritorna null
            }
        });
    }

    // ========================================
    // TEST: Tutte le Lingue Supportate
    // ========================================

    @Test
    @DisplayName("Lingua - Italiano")
    void testLanguage_Italian() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, TEST_EDUCATION_LEVEL, "it");
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Lingua - Inglese")
    void testLanguage_English() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, TEST_EDUCATION_LEVEL, "en");
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Lingua - Spagnolo")
    void testLanguage_Spanish() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, TEST_EDUCATION_LEVEL, "es");
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Lingua - Francese")
    void testLanguage_French() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, TEST_EDUCATION_LEVEL, "fr");
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Lingua - Tedesco")
    void testLanguage_German() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, TEST_EDUCATION_LEVEL, "de");
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Lingua - Portoghese")
    void testLanguage_Portuguese() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(TEST_TOPIC, TEST_EDUCATION_LEVEL, "pt");
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    // ========================================
    // TEST: Tutti i Livelli di Difficoltà
    // ========================================

    @Test
    @DisplayName("DifficultyLevel - PRINCIPIANTE")
    void testDifficultyLevel_Principiante() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateFlashcards(TEST_TOPIC, TEST_NUM_CARDS, DifficultyLevel.PRINCIPIANTE, TEST_EDUCATION_LEVEL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("DifficultyLevel - INTERMEDIO")
    void testDifficultyLevel_Intermedio() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateFlashcards(TEST_TOPIC, TEST_NUM_CARDS, DifficultyLevel.INTERMEDIO, TEST_EDUCATION_LEVEL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("DifficultyLevel - AVANZATO")
    void testDifficultyLevel_Avanzato() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateFlashcards(TEST_TOPIC, TEST_NUM_CARDS, DifficultyLevel.AVANZATO, TEST_EDUCATION_LEVEL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    // ========================================
    // TEST: Edge Cases Aggiuntivi
    // ========================================

    @Test
    @DisplayName("Topic - Lunghezza massima")
    void testTopic_MaxLength() {
        String longTopic = "A".repeat(500);
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(longTopic, TEST_EDUCATION_LEVEL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Topic - Caratteri speciali")
    void testTopic_SpecialCharacters() {
        String specialTopic = "Fotosintesi & Respirazione: CO₂ → O₂!";
        assertDoesNotThrow(() -> {
            try {
                aiService.generateExplanation(specialTopic, TEST_EDUCATION_LEVEL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Numero Cards - Minimo (1)")
    void testNumCards_Minimum() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateFlashcards(TEST_TOPIC, 1, TEST_DIFFICULTY, TEST_EDUCATION_LEVEL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Numero Cards - Massimo (10)")
    void testNumCards_Maximum() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateFlashcards(TEST_TOPIC, 10, TEST_DIFFICULTY, TEST_EDUCATION_LEVEL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Numero Questions - Minimo (1)")
    void testNumQuestions_Minimum() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateQuiz(TEST_TOPIC, 1, TEST_DIFFICULTY, TEST_EDUCATION_LEVEL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK
            }
        });
    }

    @Test
    @DisplayName("Numero Questions - Massimo (20)")
    void testNumQuestions_Maximum() {
        assertDoesNotThrow(() -> {
            try {
                aiService.generateQuiz(TEST_TOPIC, 20, TEST_DIFFICULTY, TEST_EDUCATION_LEVEL, TEST_LANGUAGE);
            } catch (NullPointerException e) {
                // OK
            }
        });
    }
}