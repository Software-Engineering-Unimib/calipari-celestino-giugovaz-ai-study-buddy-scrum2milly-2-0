package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.explanation.ExplanationResponse;
import com.ai.studybuddy.dto.gamification.GamificationDTO.XpEventResponse;
import com.ai.studybuddy.model.gamification.Badge;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.inter.AIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExplanationServiceImplTest {

    @Mock
    private AIService aiService;

    @Mock
    private GamificationServiceImpl gamificationService;

    @InjectMocks
    private ExplanationServiceImpl explanationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
    }

    @Test
    void generateExplanation_shouldReturnCorrectResponse() {
        // Arrange
        String topic = "Equazioni";
        String level = "superiore";
        String subject = "Matematica";

        when(aiService.generateExplanation(topic, "scuola superiore"))
                .thenReturn("Spiegazione AI");

        // Mock UserStats (contenuto dentro XpEventResponse)
        UserStats stats = mock(UserStats.class);
        when(stats.getTotalXp()).thenReturn(100);
        when(stats.getLevel()).thenReturn(2);

        // Crea un vero XpEventResponse (NO mock)
        XpEventResponse xpResponse =
                new XpEventResponse("EXPLANATION", 10, stats, true, List.of());

        when(gamificationService.recordExplanationXp(user, topic, subject))
                .thenReturn(xpResponse);

        // Act
        ExplanationResponse response =
                explanationService.generateExplanation(topic, level, subject, user);

        // Assert
        assertNotNull(response);
        assertEquals(topic, response.getTopic());
        assertEquals(level, response.getLevel());
        assertEquals(subject, response.getSubject());
        assertEquals("Spiegazione AI", response.getExplanation());
        assertEquals(10, response.getXpEarned());
        assertEquals(100, response.getTotalXp());
        assertEquals(2, response.getNewLevel());
        assertTrue(response.isLeveledUp());

        verify(aiService).generateExplanation(topic, "scuola superiore");
        verify(gamificationService).recordExplanationXp(user, topic, subject);
    }

    @Test
    void generateExplanationPreview_shouldCallAIServiceWithMappedLevel() {
        // Arrange
        when(aiService.generateExplanation("Fotosintesi", "scuola media"))
                .thenReturn("Preview AI");

        // Act
        String result =
                explanationService.generateExplanationPreview("Fotosintesi", "media");

        // Assert
        assertEquals("Preview AI", result);
        verify(aiService).generateExplanation("Fotosintesi", "scuola media");
    }

    @Test
    void generateExplanation_shouldDefaultToUniversita_whenLevelIsNull() {
        // Arrange
        when(aiService.generateExplanation("Derivate", "università"))
                .thenReturn("AI Response");

        UserStats stats = mock(UserStats.class);
        when(stats.getTotalXp()).thenReturn(50);
        when(stats.getLevel()).thenReturn(1);

        XpEventResponse xpResponse =
                new XpEventResponse("EXPLANATION", 10, stats, false, List.of());

        when(gamificationService.recordExplanationXp(any(), any(), any()))
                .thenReturn(xpResponse);

        // Act
        ExplanationResponse response =
                explanationService.generateExplanation("Derivate", null, "Matematica", user);

        // Assert
        assertEquals("AI Response", response.getExplanation());
        verify(aiService).generateExplanation("Derivate", "università");
    }
}