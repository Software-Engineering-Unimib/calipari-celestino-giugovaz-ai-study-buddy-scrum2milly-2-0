package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.explanation.ExplanationResponse;
import com.ai.studybuddy.dto.gamification.GamificationDTO.XpEventResponse;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.inter.AIService;
import com.ai.studybuddy.util.enums.EducationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExplanationServiceImpl - Test Suite Completo")
class ExplanationServiceImplTest {

    @Mock
    private AIService aiService;

    @Mock
    private GamificationServiceImpl gamificationService;

    @InjectMocks
    private ExplanationServiceImpl explanationService;

    private User testUser;
    private static final String TEST_TOPIC = "Fotosintesi";
    private static final String TEST_SUBJECT = "Biologia";
    private static final String TEST_EXPLANATION = "La fotosintesi è il processo mediante il quale...";

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setPreferredLanguage("it");
        user.setEducationLevel(EducationLevel.UNIVERSITY);
        user.setTotalPoints(100);
        user.setLevel(2);
        return user;
    }

    private XpEventResponse createXpEventResponse() {
        UserStats stats = new UserStats();
        stats.setTotalXp(110);
        stats.setLevel(2);
        
        return new XpEventResponse("EXPLANATION", 10, stats, false, new ArrayList<>());
    }

    // ========================================
    // TEST: generateExplanation
    // ========================================

    @Test
    @DisplayName("generateExplanation - Successo con tutti i parametri")
    void testGenerateExplanation_Success() {
        // Arrange
        String level = "university";
        XpEventResponse xpEvent = createXpEventResponse();

        when(aiService.generateExplanation(TEST_TOPIC, EducationLevel.UNIVERSITY, "it"))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(testUser, TEST_TOPIC, TEST_SUBJECT))
                .thenReturn(xpEvent);

        // Act
        ExplanationResponse result = explanationService.generateExplanation(
                TEST_TOPIC, level, TEST_SUBJECT, testUser);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TOPIC, result.getTopic());
        assertEquals(level, result.getLevel());
        assertEquals(TEST_SUBJECT, result.getSubject());
        assertEquals(TEST_EXPLANATION, result.getExplanation());
        assertEquals(10, result.getXpEarned());
        assertEquals(110, result.getTotalXp());
        assertFalse(result.isLeveledUp());

        verify(aiService, times(1)).generateExplanation(
                eq(TEST_TOPIC), eq(EducationLevel.UNIVERSITY), eq("it"));
        verify(gamificationService, times(1)).recordExplanationXp(testUser, TEST_TOPIC, TEST_SUBJECT);
    }

    @Test
    @DisplayName("generateExplanation - Usa lingua preferita dell'utente")
    void testGenerateExplanation_UsesUserLanguage() {
        // Arrange
        testUser.setPreferredLanguage("en");
        XpEventResponse xpEvent = createXpEventResponse();

        when(aiService.generateExplanation(anyString(), any(), eq("en")))
                .thenReturn("Photosynthesis is the process...");
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(xpEvent);

        // Act
        explanationService.generateExplanation(TEST_TOPIC, "high", TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), any(EducationLevel.class), eq("en"));
    }

    @Test
    @DisplayName("generateExplanation - Level up durante spiegazione")
    void testGenerateExplanation_LevelUp() {
        // Arrange
        UserStats stats = new UserStats();
        stats.setTotalXp(200);
        stats.setLevel(3);
        
        XpEventResponse xpEvent = new XpEventResponse("EXPLANATION", 10, stats, true, new ArrayList<>());

        when(aiService.generateExplanation(anyString(), any(), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(testUser, TEST_TOPIC, TEST_SUBJECT))
                .thenReturn(xpEvent);

        // Act
        ExplanationResponse result = explanationService.generateExplanation(
                TEST_TOPIC, "university", TEST_SUBJECT, testUser);

        // Assert
        assertTrue(result.isLeveledUp());
        assertEquals(3, result.getNewLevel());
        assertEquals(200, result.getTotalXp());
    }

    // ========================================
    // TEST: generateExplanationPreview
    // ========================================

    @Test
    @DisplayName("generateExplanationPreview - Genera preview senza XP")
    void testGenerateExplanationPreview_Success() {
        // Arrange
        String level = "high";
        when(aiService.generateExplanation(TEST_TOPIC, EducationLevel.HIGH_SCHOOL, "it"))
                .thenReturn(TEST_EXPLANATION);

        // Act
        String result = explanationService.generateExplanationPreview(TEST_TOPIC, level);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_EXPLANATION, result);
        verify(aiService, times(1)).generateExplanation(
                eq(TEST_TOPIC), eq(EducationLevel.HIGH_SCHOOL), eq("it"));
        verify(gamificationService, never()).recordExplanationXp(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("generateExplanationPreview - Usa sempre italiano")
    void testGenerateExplanationPreview_AlwaysItalian() {
        // Arrange
        when(aiService.generateExplanation(anyString(), any(), eq("it")))
                .thenReturn("Preview in italiano");

        // Act
        explanationService.generateExplanationPreview("Topic", "middle");

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), any(EducationLevel.class), eq("it"));
    }

    // ========================================
    // TEST: Mapping Livelli Educativi
    // ========================================

    @Test
    @DisplayName("mapLevel - UNIVERSITY mapping corretto")
    void testMapLevel_University() {
        // Arrange
        when(aiService.generateExplanation(anyString(), eq(EducationLevel.UNIVERSITY), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(createXpEventResponse());

        // Act
        explanationService.generateExplanation(TEST_TOPIC, "university", TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), eq(EducationLevel.UNIVERSITY), anyString());
    }

    @Test
    @DisplayName("mapLevel - UNIVERSITY variante italiana")
    void testMapLevel_Universita() {
        // Arrange
        when(aiService.generateExplanation(anyString(), eq(EducationLevel.UNIVERSITY), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(createXpEventResponse());

        // Act
        explanationService.generateExplanation(TEST_TOPIC, "università", TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), eq(EducationLevel.UNIVERSITY), anyString());
    }

    @Test
    @DisplayName("mapLevel - HIGH_SCHOOL mapping corretto")
    void testMapLevel_HighSchool() {
        // Arrange
        when(aiService.generateExplanation(anyString(), eq(EducationLevel.HIGH_SCHOOL), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(createXpEventResponse());

        // Act
        explanationService.generateExplanation(TEST_TOPIC, "high", TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), eq(EducationLevel.HIGH_SCHOOL), anyString());
    }

    @Test
    @DisplayName("mapLevel - HIGH_SCHOOL variante italiana")
    void testMapLevel_Superiore() {
        // Arrange
        when(aiService.generateExplanation(anyString(), eq(EducationLevel.HIGH_SCHOOL), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(createXpEventResponse());

        // Act
        explanationService.generateExplanation(TEST_TOPIC, "superiore", TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), eq(EducationLevel.HIGH_SCHOOL), anyString());
    }

    @Test
    @DisplayName("mapLevel - MIDDLE_SCHOOL mapping corretto")
    void testMapLevel_MiddleSchool() {
        // Arrange
        when(aiService.generateExplanation(anyString(), eq(EducationLevel.MIDDLE_SCHOOL), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(createXpEventResponse());

        // Act
        explanationService.generateExplanation(TEST_TOPIC, "middle", TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), eq(EducationLevel.MIDDLE_SCHOOL), anyString());
    }

    @Test
    @DisplayName("mapLevel - MIDDLE_SCHOOL variante italiana")
    void testMapLevel_Media() {
        // Arrange
        when(aiService.generateExplanation(anyString(), eq(EducationLevel.MIDDLE_SCHOOL), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(createXpEventResponse());

        // Act
        explanationService.generateExplanation(TEST_TOPIC, "media", TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), eq(EducationLevel.MIDDLE_SCHOOL), anyString());
    }

    @Test
    @DisplayName("mapLevel - Valore null usa default UNIVERSITY")
    void testMapLevel_NullUsesDefault() {
        // Arrange
        when(aiService.generateExplanation(anyString(), eq(EducationLevel.UNIVERSITY), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(createXpEventResponse());

        // Act
        explanationService.generateExplanation(TEST_TOPIC, null, TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), eq(EducationLevel.UNIVERSITY), anyString());
    }

    @Test
    @DisplayName("mapLevel - Valore vuoto usa default UNIVERSITY")
    void testMapLevel_EmptyUsesDefault() {
        // Arrange
        when(aiService.generateExplanation(anyString(), eq(EducationLevel.UNIVERSITY), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(createXpEventResponse());

        // Act
        explanationService.generateExplanation(TEST_TOPIC, "", TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), eq(EducationLevel.UNIVERSITY), anyString());
    }

    @Test
    @DisplayName("mapLevel - Valore non riconosciuto usa default UNIVERSITY")
    void testMapLevel_UnknownUsesDefault() {
        // Arrange
        when(aiService.generateExplanation(anyString(), eq(EducationLevel.UNIVERSITY), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(createXpEventResponse());

        // Act
        explanationService.generateExplanation(TEST_TOPIC, "unknown_level", TEST_SUBJECT, testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), eq(EducationLevel.UNIVERSITY), anyString());
    }

    // ========================================
    // TEST: Integrazione con Gamification
    // ========================================

    @Test
    @DisplayName("generateExplanation - Registra XP correttamente")
    void testGenerateExplanation_RecordsXp() {
        // Arrange
        XpEventResponse xpEvent = createXpEventResponse();
        when(aiService.generateExplanation(anyString(), any(), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(testUser, TEST_TOPIC, TEST_SUBJECT))
                .thenReturn(xpEvent);

        // Act
        ExplanationResponse result = explanationService.generateExplanation(
                TEST_TOPIC, "university", TEST_SUBJECT, testUser);

        // Assert
        assertEquals(10, result.getXpEarned());
        verify(gamificationService, times(1)).recordExplanationXp(
                eq(testUser), eq(TEST_TOPIC), eq(TEST_SUBJECT));
    }

    @Test
    @DisplayName("generateExplanation - Include nuovi badge se presenti")
    void testGenerateExplanation_WithNewBadges() {
        // Arrange
        UserStats stats = new UserStats();
        stats.setTotalXp(110);
        stats.setLevel(2);
        
        XpEventResponse xpEvent = new XpEventResponse("EXPLANATION", 10, stats, false, new ArrayList<>());

        when(aiService.generateExplanation(anyString(), any(), anyString()))
                .thenReturn(TEST_EXPLANATION);
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(xpEvent);

        // Act
        ExplanationResponse result = explanationService.generateExplanation(
                TEST_TOPIC, "university", TEST_SUBJECT, testUser);

        // Assert
        assertNotNull(result.getNewBadges());
    }

    // ========================================
    // TEST: Lingue Multiple
    // ========================================

    @Test
    @DisplayName("generateExplanation - Supporta spagnolo")
    void testGenerateExplanation_Spanish() {
        // Arrange
        testUser.setPreferredLanguage("es");
        XpEventResponse xpEvent = createXpEventResponse();

        when(aiService.generateExplanation(anyString(), any(), eq("es")))
                .thenReturn("La fotosíntesis es el proceso...");
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(xpEvent);

        // Act
        explanationService.generateExplanation("Fotosíntesis", "university", "Biología", testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), any(EducationLevel.class), eq("es"));
    }

    @Test
    @DisplayName("generateExplanation - Supporta francese")
    void testGenerateExplanation_French() {
        // Arrange
        testUser.setPreferredLanguage("fr");
        XpEventResponse xpEvent = createXpEventResponse();

        when(aiService.generateExplanation(anyString(), any(), eq("fr")))
                .thenReturn("La photosynthèse est le processus...");
        when(gamificationService.recordExplanationXp(any(), anyString(), anyString()))
                .thenReturn(xpEvent);

        // Act
        explanationService.generateExplanation("Photosynthèse", "university", "Biologie", testUser);

        // Assert
        verify(aiService, times(1)).generateExplanation(
                anyString(), any(EducationLevel.class), eq("fr"));
    }
}