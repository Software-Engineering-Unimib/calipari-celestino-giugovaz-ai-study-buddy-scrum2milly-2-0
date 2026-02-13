package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.flashcard.FlashcardAIGenerateRequest;
import com.ai.studybuddy.dto.flashcard.FlashcardCreateRequest;
import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.exception.UnauthorizedException;
import com.ai.studybuddy.mapper.FlashcardMapper;
import com.ai.studybuddy.model.flashcard.Flashcard;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.FlashcardDeckRepository;
import com.ai.studybuddy.repository.FlashcardRepository;
import com.ai.studybuddy.service.inter.AIService;
import com.ai.studybuddy.service.inter.FlashcardService;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.ai.studybuddy.util.enums.EducationLevel;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlashcardServiceImpl - Test Suite Completo")
class FlashcardServiceImplTest {

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private FlashcardDeckRepository deckRepository;

    @Mock
    private AIService aiService;

    @Mock
    private FlashcardMapper flashcardMapper;

    @Mock
    private FlashcardService selfProxy;

    @InjectMocks
    private FlashcardServiceImpl flashcardService;

    private User testUser;
    private FlashcardDeck testDeck;
    private Flashcard testFlashcard;
    private UUID userId;
    private UUID deckId;
    private UUID flashcardId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        deckId = UUID.randomUUID();
        flashcardId = UUID.randomUUID();

        testUser = createTestUser();
        testDeck = createTestDeck();
        testFlashcard = createTestFlashcard();

        flashcardService.setSelfProxy(selfProxy);
    }

    private User createTestUser() {
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPreferredLanguage("it");
        user.setEducationLevel(EducationLevel.UNIVERSITY);
        return user;
    }

    private FlashcardDeck createTestDeck() {
        FlashcardDeck deck = new FlashcardDeck();
        deck.setId(deckId);
        deck.setName("Test Deck");
        deck.setOwner(testUser);
        deck.setTotalCards(0);
        deck.setCreatedAt(LocalDateTime.now());
        return deck;
    }

    private Flashcard createTestFlashcard() {
        Flashcard flashcard = new Flashcard();
        flashcard.setId(flashcardId);
        flashcard.setDeck(testDeck);
        flashcard.setCreatedBy(testUser);
        flashcard.setFrontContent("Domanda test");
        flashcard.setBackContent("Risposta test");
        flashcard.setDifficultyLevel(DifficultyLevel.INTERMEDIO);
        flashcard.setIsActive(true);
        flashcard.setTimesReviewed(0);
        flashcard.setTimesCorrect(0);
        flashcard.setAiGenerated(false);
        return flashcard;
    }

    // ========================================
    // TEST: createFlashcard
    // ========================================

    @Test
    @DisplayName("createFlashcard - Successo")
    void testCreateFlashcard_Success() {
        // Arrange
        FlashcardCreateRequest request = FlashcardCreateRequest.builder()
                .frontContent("Nuova domanda")
                .backContent("Nuova risposta")
                .difficultyLevel(DifficultyLevel.PRINCIPIANTE)
                .build();

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(flashcardMapper.toEntity(request, testDeck, testUser)).thenReturn(testFlashcard);
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        Flashcard result = flashcardService.createFlashcard(deckId, request, testUser);

        // Assert
        assertNotNull(result);
        verify(flashcardRepository, times(1)).save(testFlashcard);
        verify(deckRepository, times(1)).save(testDeck);
        assertEquals(1, testDeck.getTotalCards());
    }

    @Test
    @DisplayName("createFlashcard - Deck non trovato")
    void testCreateFlashcard_DeckNotFound() {
        // Arrange
        FlashcardCreateRequest request = FlashcardCreateRequest.builder()
                .frontContent("Test")
                .backContent("Test")
                .build();

        when(deckRepository.findById(deckId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            flashcardService.createFlashcard(deckId, request, testUser);
        });
    }

    @Test
    @DisplayName("createFlashcard - Utente non autorizzato")
    void testCreateFlashcard_Unauthorized() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        
        FlashcardCreateRequest request = FlashcardCreateRequest.builder()
                .frontContent("Test")
                .backContent("Test")
                .build();

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            flashcardService.createFlashcard(deckId, request, otherUser);
        });
    }

    // ========================================
    // TEST: generateAndSaveFlashcards
    // ========================================

    @Test
    @DisplayName("generateAndSaveFlashcards - Successo con lingua specificata")
    void testGenerateAndSaveFlashcards_WithLanguage() {
        // Arrange
        FlashcardAIGenerateRequest request = FlashcardAIGenerateRequest.builder()
                .topic("Fotosintesi")
                .numberOfCards(3)
                .difficultyLevel(DifficultyLevel.INTERMEDIO)
                .language("it")
                .build();

        String aiResponse = """
            [
                {"front": "Domanda 1", "back": "Risposta 1"},
                {"front": "Domanda 2", "back": "Risposta 2"},
                {"front": "Domanda 3", "back": "Risposta 3"}
            ]
            """;

        JsonArray jsonArray = JsonParser.parseString(aiResponse).getAsJsonArray();

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(aiService.generateFlashcards(anyString(), anyInt(), any(), any(), anyString()))
                .thenReturn(aiResponse);
        when(aiService.parseFlashcardsResponse(aiResponse)).thenReturn(jsonArray);
        when(flashcardMapper.toAIGeneratedEntity(any(), any(), any())).thenReturn(testFlashcard);
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        List<Flashcard> result = flashcardService.generateAndSaveFlashcards(deckId, request, testUser);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(aiService, times(1)).generateFlashcards(
                eq("Fotosintesi"), eq(3), eq(DifficultyLevel.INTERMEDIO), 
                eq(EducationLevel.UNIVERSITY), eq("it"));
        verify(flashcardRepository, times(3)).save(any(Flashcard.class));
    }

    @Test
    @DisplayName("generateAndSaveFlashcards - Con contesto")
    void testGenerateAndSaveFlashcards_WithContext() {
        // Arrange
        String context = "Focalizzati sul processo di conversione della luce";
        FlashcardAIGenerateRequest request = FlashcardAIGenerateRequest.builder()
                .topic("Fotosintesi")
                .numberOfCards(2)
                .difficultyLevel(DifficultyLevel.AVANZATO)
                .context(context)
                .language("en")
                .build();

        String aiResponse = "[{\"front\": \"Q1\", \"back\": \"A1\"},{\"front\": \"Q2\", \"back\": \"A2\"}]";
        JsonArray jsonArray = JsonParser.parseString(aiResponse).getAsJsonArray();

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(aiService.generateFlashcardsWithContext(anyString(), anyInt(), any(), anyString(), anyString()))
                .thenReturn(aiResponse);
        when(aiService.parseFlashcardsResponse(aiResponse)).thenReturn(jsonArray);
        when(flashcardMapper.toAIGeneratedEntity(any(), any(), any())).thenReturn(testFlashcard);
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        List<Flashcard> result = flashcardService.generateAndSaveFlashcards(deckId, request, testUser);

        // Assert
        assertEquals(2, result.size());
        verify(aiService, times(1)).generateFlashcardsWithContext(
                eq("Fotosintesi"), eq(2), eq(DifficultyLevel.AVANZATO), eq(context), eq("en"));
    }

    @Test
    @DisplayName("generateAndSaveFlashcards - Usa lingua utente se non specificata")
    void testGenerateAndSaveFlashcards_DefaultUserLanguage() {
        // Arrange
        FlashcardAIGenerateRequest request = FlashcardAIGenerateRequest.builder()
                .topic("Test Topic")
                .numberOfCards(1)
                .difficultyLevel(DifficultyLevel.PRINCIPIANTE)
                .language(null) // Lingua non specificata
                .build();

        String aiResponse = "[{\"front\": \"Q\", \"back\": \"A\"}]";
        JsonArray jsonArray = JsonParser.parseString(aiResponse).getAsJsonArray();

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(aiService.generateFlashcards(anyString(), anyInt(), any(), any(), eq("it")))
                .thenReturn(aiResponse);
        when(aiService.parseFlashcardsResponse(aiResponse)).thenReturn(jsonArray);
        when(flashcardMapper.toAIGeneratedEntity(any(), any(), any())).thenReturn(testFlashcard);
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        flashcardService.generateAndSaveFlashcards(deckId, request, testUser);

        // Assert
        verify(aiService, times(1)).generateFlashcards(anyString(), anyInt(), any(), any(), eq("it"));
    }

    @Test
    @DisplayName("generateAndSaveFlashcards (deprecated) - Usa lingua utente")
    void testGenerateAndSaveFlashcardsDeprecated_UsesUserLanguage() {
        // Arrange
        List<Flashcard> expectedResult = Arrays.asList(testFlashcard);
        when(selfProxy.generateAndSaveFlashcards(any(), any(), any())).thenReturn(expectedResult);

        // Act
        List<Flashcard> result = flashcardService.generateAndSaveFlashcards(
                deckId, "Topic", 3, "medium", testUser);

        // Assert
        assertNotNull(result);
        verify(selfProxy, times(1)).generateAndSaveFlashcards(any(), any(), any());
    }

    // ========================================
    // TEST: getFlashcardsByDeck
    // ========================================

    @Test
    @DisplayName("getFlashcardsByDeck - Restituisce flashcards attive")
    void testGetFlashcardsByDeck_Success() {
        // Arrange
        List<Flashcard> flashcards = Arrays.asList(testFlashcard, createTestFlashcard());
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(flashcardRepository.findByDeckIdAndIsActiveTrue(deckId)).thenReturn(flashcards);

        // Act
        List<Flashcard> result = flashcardService.getFlashcardsByDeck(deckId, userId);

        // Assert
        assertEquals(2, result.size());
        verify(flashcardRepository, times(1)).findByDeckIdAndIsActiveTrue(deckId);
    }

    // ========================================
    // TEST: reviewFlashcard
    // ========================================

    @Test
    @DisplayName("reviewFlashcard - Risposta corretta")
    void testReviewFlashcard_Correct() {
        // Arrange
        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(testFlashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        // Act
        Flashcard result = flashcardService.reviewFlashcard(flashcardId, true, userId);

        // Assert
        assertNotNull(result);
        verify(flashcardRepository, times(1)).save(testFlashcard);
    }

    @Test
    @DisplayName("reviewFlashcard - Risposta errata")
    void testReviewFlashcard_Incorrect() {
        // Arrange
        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(testFlashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        // Act
        Flashcard result = flashcardService.reviewFlashcard(flashcardId, false, userId);

        // Assert
        assertNotNull(result);
        verify(flashcardRepository, times(1)).save(testFlashcard);
    }

    @Test
    @DisplayName("reviewFlashcard - Flashcard non trovata")
    void testReviewFlashcard_NotFound() {
        // Arrange
        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            flashcardService.reviewFlashcard(flashcardId, true, userId);
        });
    }

    // ========================================
    // TEST: getStudySession
    // ========================================

    @Test
    @DisplayName("getStudySession - Restituisce carte mai riviste")
    void testGetStudySession_NeverReviewed() {
        // Arrange
        int numberOfCards = 5;
        List<Flashcard> neverReviewed = Arrays.asList(
                testFlashcard, createTestFlashcard(), createTestFlashcard(),
                createTestFlashcard(), createTestFlashcard()
        );
        
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(flashcardRepository.findNeverReviewedByDeckId(deckId)).thenReturn(neverReviewed);

        // Act
        List<Flashcard> result = flashcardService.getStudySession(deckId, numberOfCards, userId);

        // Assert
        assertEquals(5, result.size());
        verify(flashcardRepository, times(1)).findNeverReviewedByDeckId(deckId);
    }

    @Test
    @DisplayName("getStudySession - Restituisce carte che necessitano ripasso")
    void testGetStudySession_NeedingReview() {
        // Arrange
        int numberOfCards = 3;
        List<Flashcard> needReview = Arrays.asList(testFlashcard, createTestFlashcard(), createTestFlashcard());
        
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(flashcardRepository.findNeverReviewedByDeckId(deckId)).thenReturn(Arrays.asList());
        when(flashcardRepository.findNeedingReview(eq(deckId), any(LocalDateTime.class)))
                .thenReturn(needReview);

        // Act
        List<Flashcard> result = flashcardService.getStudySession(deckId, numberOfCards, userId);

        // Assert
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("getStudySession - Restituisce carte casuali")
    void testGetStudySession_Random() {
        // Arrange
        int numberOfCards = 2;
        List<Flashcard> randomCards = Arrays.asList(testFlashcard, createTestFlashcard());
        
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(flashcardRepository.findNeverReviewedByDeckId(deckId)).thenReturn(Arrays.asList());
        when(flashcardRepository.findNeedingReview(eq(deckId), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());
        when(flashcardRepository.findRandomForStudy(deckId, numberOfCards)).thenReturn(randomCards);

        // Act
        List<Flashcard> result = flashcardService.getStudySession(deckId, numberOfCards, userId);

        // Assert
        assertEquals(2, result.size());
        verify(flashcardRepository, times(1)).findRandomForStudy(deckId, numberOfCards);
    }

    // ========================================
    // TEST: updateFlashcard
    // ========================================

    @Test
    @DisplayName("updateFlashcard - Successo")
    void testUpdateFlashcard_Success() {
        // Arrange
        FlashcardCreateRequest request = FlashcardCreateRequest.builder()
                .frontContent("Domanda aggiornata")
                .backContent("Risposta aggiornata")
                .build();

        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(testFlashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        // Act
        Flashcard result = flashcardService.updateFlashcard(flashcardId, request, userId);

        // Assert
        assertNotNull(result);
        verify(flashcardMapper, times(1)).updateEntity(testFlashcard, request);
        verify(flashcardRepository, times(1)).save(testFlashcard);
    }

    // ========================================
    // TEST: deleteFlashcard
    // ========================================

    @Test
    @DisplayName("deleteFlashcard - Soft delete")
    void testDeleteFlashcard_Success() {
        // Arrange
        testDeck.setTotalCards(5);
        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(testFlashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        flashcardService.deleteFlashcard(flashcardId, userId);

        // Assert
        verify(flashcardRepository, times(1)).save(testFlashcard);
        assertEquals(4, testDeck.getTotalCards());
    }

    // ========================================
    // TEST: searchFlashcards
    // ========================================

    @Test
    @DisplayName("searchFlashcards - Ricerca per contenuto")
    void testSearchFlashcards_Success() {
        // Arrange
        String searchTerm = "fotosintesi";
        List<Flashcard> searchResults = Arrays.asList(testFlashcard);
        
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(flashcardRepository.searchByContent(deckId, searchTerm)).thenReturn(searchResults);

        // Act
        List<Flashcard> result = flashcardService.searchFlashcards(deckId, searchTerm, userId);

        // Assert
        assertEquals(1, result.size());
        verify(flashcardRepository, times(1)).searchByContent(deckId, searchTerm);
    }

    // ========================================
    // TEST: getFlashcardStats
    // ========================================

    @Test
    @DisplayName("getFlashcardStats - Calcola statistiche correttamente")
    void testGetFlashcardStats_Success() {
        // Arrange
        Flashcard masteredCard = createTestFlashcard();
        masteredCard.setTimesReviewed(10);
        masteredCard.setTimesCorrect(9); // 90% success rate
        
        Flashcard needsReviewCard = createTestFlashcard();
        needsReviewCard.setTimesReviewed(0);

        List<Flashcard> allCards = Arrays.asList(masteredCard, needsReviewCard, testFlashcard);
        
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(testDeck));
        when(flashcardRepository.findByDeckIdAndIsActiveTrue(deckId)).thenReturn(allCards);

        // Act
        FlashcardService.FlashcardStats result = flashcardService.getFlashcardStats(deckId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotal());
    }
}
