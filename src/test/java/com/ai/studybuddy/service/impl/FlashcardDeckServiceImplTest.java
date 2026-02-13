package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.flashcard.FlashcardDeckCreateRequest;
import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.mapper.FlashcardMapper;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.FlashcardDeckRepository;
import com.ai.studybuddy.service.inter.FlashcardDeckService;
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
@DisplayName("FlashcardDeckServiceImpl - Test Suite Completo")
class FlashcardDeckServiceImplTest {

    @Mock
    private FlashcardDeckRepository deckRepository;

    @Mock
    private FlashcardMapper flashcardMapper;

    @InjectMocks
    private FlashcardDeckServiceImpl flashcardDeckService;

    private User testUser;
    private FlashcardDeck testDeck;
    private UUID userId;
    private UUID deckId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        deckId = UUID.randomUUID();
        testUser = createTestUser();
        testDeck = createTestDeck();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    private FlashcardDeck createTestDeck() {
        FlashcardDeck deck = new FlashcardDeck();
        deck.setId(deckId);
        deck.setName("Test Deck");
        deck.setDescription("Test Description");
        deck.setOwner(testUser);
        deck.setSubject("Biology");
        deck.setTotalCards(0);
        deck.setCardsMastered(0);
        deck.setTimesStudied(0);
        deck.setIsActive(true);
        deck.setIsPublic(false);
        deck.setCreatedAt(LocalDateTime.now());
        deck.setUpdatedAt(LocalDateTime.now());
        return deck;
    }

    // ========================================
    // TEST: createDeck
    // ========================================

    @Test
    @DisplayName("createDeck - Successo")
    void testCreateDeck_Success() {
        // Arrange
        FlashcardDeckCreateRequest request = FlashcardDeckCreateRequest.builder()
                .name("New Deck")
                .description("New Description")
                .subject("Mathematics")
                .isPublic(false)
                .build();

        when(flashcardMapper.toEntity(request, testUser)).thenReturn(testDeck);
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        FlashcardDeck result = flashcardDeckService.createDeck(request, testUser);

        // Assert
        assertNotNull(result);
        assertEquals(deckId, result.getId());
        assertEquals("Test Deck", result.getName());
        verify(flashcardMapper, times(1)).toEntity(request, testUser);
        verify(deckRepository, times(1)).save(testDeck);
    }

    @Test
    @DisplayName("createDeck - Con nome e descrizione personalizzati")
    void testCreateDeck_CustomDetails() {
        // Arrange
        String customName = "Advanced Physics";
        String customDescription = "Quantum mechanics and relativity";
        
        FlashcardDeckCreateRequest request = FlashcardDeckCreateRequest.builder()
                .name(customName)
                .description(customDescription)
                .subject("Physics")
                .build();

        FlashcardDeck customDeck = createTestDeck();
        customDeck.setName(customName);
        customDeck.setDescription(customDescription);

        when(flashcardMapper.toEntity(request, testUser)).thenReturn(customDeck);
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(customDeck);

        // Act
        FlashcardDeck result = flashcardDeckService.createDeck(request, testUser);

        // Assert
        assertEquals(customName, result.getName());
        assertEquals(customDescription, result.getDescription());
    }

    // ========================================
    // TEST: getUserDecks
    // ========================================

    @Test
    @DisplayName("getUserDecks - Restituisce deck attivi dell'utente")
    void testGetUserDecks_Success() {
        // Arrange
        List<FlashcardDeck> decks = Arrays.asList(testDeck, createTestDeck());
        when(deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId))
                .thenReturn(decks);

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getUserDecks(userId);

        // Assert
        assertEquals(2, result.size());
        verify(deckRepository, times(1))
                .findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
    }

    @Test
    @DisplayName("getUserDecks - Nessun deck presente")
    void testGetUserDecks_Empty() {
        // Arrange
        when(deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId))
                .thenReturn(Arrays.asList());

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getUserDecks(userId);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ========================================
    // TEST: getDeck
    // ========================================

    @Test
    @DisplayName("getDeck - Successo")
    void testGetDeck_Success() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(testDeck));

        // Act
        FlashcardDeck result = flashcardDeckService.getDeck(deckId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(deckId, result.getId());
    }

    @Test
    @DisplayName("getDeck - Deck non trovato")
    void testGetDeck_NotFound() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            flashcardDeckService.getDeck(deckId, userId);
        });
    }

    // ========================================
    // TEST: updateDeck
    // ========================================

    @Test
    @DisplayName("updateDeck - Successo")
    void testUpdateDeck_Success() {
        // Arrange
        FlashcardDeckCreateRequest request = FlashcardDeckCreateRequest.builder()
                .name("Updated Name")
                .description("Updated Description")
                .subject("Chemistry")
                .build();

        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(testDeck));
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        FlashcardDeck result = flashcardDeckService.updateDeck(deckId, request, userId);

        // Assert
        assertNotNull(result);
        verify(flashcardMapper, times(1)).updateEntity(testDeck, request);
        verify(deckRepository, times(1)).save(testDeck);
    }

    @Test
    @DisplayName("updateDeck - Deck non dell'utente")
    void testUpdateDeck_NotOwnedByUser() {
        // Arrange
        UUID otherUserId = UUID.randomUUID();
        FlashcardDeckCreateRequest request = FlashcardDeckCreateRequest.builder()
                .name("Test")
                .build();

        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, otherUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            flashcardDeckService.updateDeck(deckId, request, otherUserId);
        });
    }

    // ========================================
    // TEST: deleteDeck
    // ========================================

    @Test
    @DisplayName("deleteDeck - Soft delete")
    void testDeleteDeck_SoftDelete() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(testDeck));
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        flashcardDeckService.deleteDeck(deckId, userId);

        // Assert
        assertFalse(testDeck.getIsActive());
        verify(deckRepository, times(1)).save(testDeck);
    }

    // ========================================
    // TEST: recordStudySession
    // ========================================

    @Test
    @DisplayName("recordStudySession - Incrementa contatore")
    void testRecordStudySession_Success() {
        // Arrange
        int initialStudyCount = testDeck.getTimesStudied();
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(testDeck));
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        flashcardDeckService.recordStudySession(deckId, userId);

        // Assert
        verify(deckRepository, times(1)).save(testDeck);
    }

    // ========================================
    // TEST: updateMasteredCount
    // ========================================

    @Test
    @DisplayName("updateMasteredCount - Aggiorna conteggio")
    void testUpdateMasteredCount_Success() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(testDeck));
        when(deckRepository.save(any(FlashcardDeck.class))).thenReturn(testDeck);

        // Act
        flashcardDeckService.updateMasteredCount(deckId, userId);

        // Assert
        verify(deckRepository, times(1)).save(testDeck);
    }

    // ========================================
    // TEST: searchDecks
    // ========================================

    @Test
    @DisplayName("searchDecks - Trova deck per termine di ricerca")
    void testSearchDecks_Success() {
        // Arrange
        String searchTerm = "biology";
        List<FlashcardDeck> searchResults = Arrays.asList(testDeck);
        
        when(deckRepository.searchByName(userId, searchTerm))
                .thenReturn(searchResults);

        // Act
        List<FlashcardDeck> result = flashcardDeckService.searchDecks(userId, searchTerm);

        // Assert
        assertEquals(1, result.size());
        verify(deckRepository, times(1)).searchByName(userId, searchTerm);
    }

    @Test
    @DisplayName("searchDecks - Nessun risultato")
    void testSearchDecks_NoResults() {
        // Arrange
        when(deckRepository.searchByName(userId, "nonexistent"))
                .thenReturn(Arrays.asList());

        // Act
        List<FlashcardDeck> result = flashcardDeckService.searchDecks(userId, "nonexistent");

        // Assert
        assertTrue(result.isEmpty());
    }

    // ========================================
    // TEST: getPublicDecks
    // ========================================

    @Test
    @DisplayName("getPublicDecks - Restituisce deck pubblici")
    void testGetPublicDecks_Success() {
        // Arrange
        FlashcardDeck publicDeck = createTestDeck();
        publicDeck.setIsPublic(true);
        List<FlashcardDeck> publicDecks = Arrays.asList(publicDeck);
        
        when(deckRepository.findByIsPublicTrueAndIsActiveTrueOrderByTimesStudiedDesc())
                .thenReturn(publicDecks);

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getPublicDecks();

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsPublic());
    }

    // ========================================
    // TEST: getDecksBySubject
    // ========================================

    @Test
    @DisplayName("getDecksBySubject - Filtra per materia")
    void testGetDecksBySubject_Success() {
        // Arrange
        String subject = "Biology";
        List<FlashcardDeck> subjectDecks = Arrays.asList(testDeck);
        
        when(deckRepository.findByOwnerIdAndSubjectAndIsActiveTrueOrderByNameAsc(userId, subject))
                .thenReturn(subjectDecks);

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getDecksBySubject(userId, subject);

        // Assert
        assertEquals(1, result.size());
        assertEquals(subject, result.get(0).getSubject());
    }

    @Test
    @DisplayName("getDecksBySubject - Materie diverse")
    void testGetDecksBySubject_MultipleSubjects() {
        // Arrange
        String[] subjects = {"Mathematics", "Physics", "Chemistry", "History"};
        
        for (String subject : subjects) {
            FlashcardDeck deck = createTestDeck();
            deck.setSubject(subject);
            when(deckRepository.findByOwnerIdAndSubjectAndIsActiveTrueOrderByNameAsc(userId, subject))
                    .thenReturn(Arrays.asList(deck));

            // Act
            List<FlashcardDeck> result = flashcardDeckService.getDecksBySubject(userId, subject);

            // Assert
            assertEquals(1, result.size());
            assertEquals(subject, result.get(0).getSubject());
        }
    }

    // ========================================
    // TEST: getDecksNeedingReview
    // ========================================

    @Test
    @DisplayName("getDecksNeedingReview - Deck non studiati di recente")
    void testGetDecksNeedingReview_Success() {
        // Arrange
        int daysAgo = 7;
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysAgo);
        testDeck.setUpdatedAt(LocalDateTime.now().minusDays(10)); // Vecchio
        
        when(deckRepository.findNeedingReview(eq(userId), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testDeck));

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getDecksNeedingReview(userId, daysAgo);

        // Assert
        assertEquals(1, result.size());
        verify(deckRepository, times(1)).findNeedingReview(eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getDecksNeedingReview - Nessun deck necessita ripasso")
    void testGetDecksNeedingReview_NoDecks() {
        // Arrange
        when(deckRepository.findNeedingReview(eq(userId), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getDecksNeedingReview(userId, 7);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ========================================
    // TEST: getGlobalStats
    // ========================================

    @Test
    @DisplayName("getGlobalStats - Calcola statistiche globali")
    void testGetGlobalStats_Success() {
        // Arrange
        long totalDecks = 5L;
        long totalCards = 100L;
        
        testDeck.setCardsMastered(20);
        testDeck.setTimesStudied(15);
        
        FlashcardDeck deck2 = createTestDeck();
        deck2.setCardsMastered(30);
        deck2.setTimesStudied(25);
        
        List<FlashcardDeck> decks = Arrays.asList(testDeck, deck2);
        
        when(deckRepository.countByOwnerIdAndIsActiveTrue(userId)).thenReturn(totalDecks);
        when(deckRepository.countTotalCardsByOwner(userId)).thenReturn(totalCards);
        when(deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId))
                .thenReturn(decks);

        // Act
        FlashcardDeckService.DeckGlobalStats result = flashcardDeckService.getGlobalStats(userId);

        // Assert
        assertNotNull(result);
        assertEquals(totalDecks, result.getTotalDecks());
        assertEquals(totalCards, result.getTotalCards());
        assertEquals(50L, result.getTotalMastered()); // 20 + 30
        assertEquals(40L, result.getTotalStudySessions()); // 15 + 25
    }

    @Test
    @DisplayName("getGlobalStats - Utente senza deck")
    void testGetGlobalStats_NoDecks() {
        // Arrange
        when(deckRepository.countByOwnerIdAndIsActiveTrue(userId)).thenReturn(0L);
        when(deckRepository.countTotalCardsByOwner(userId)).thenReturn(0L);
        when(deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId))
                .thenReturn(Arrays.asList());

        // Act
        FlashcardDeckService.DeckGlobalStats result = flashcardDeckService.getGlobalStats(userId);

        // Assert
        assertEquals(0L, result.getTotalDecks());
        assertEquals(0L, result.getTotalCards());
        assertEquals(0L, result.getTotalMastered());
        assertEquals(0L, result.getTotalStudySessions());
    }

    @Test
    @DisplayName("getGlobalStats - Calcolo corretto con molti deck")
    void testGetGlobalStats_ManyDecks() {
        // Arrange
        FlashcardDeck deck1 = createTestDeck();
        deck1.setCardsMastered(10);
        deck1.setTimesStudied(5);
        
        FlashcardDeck deck2 = createTestDeck();
        deck2.setCardsMastered(20);
        deck2.setTimesStudied(10);
        
        FlashcardDeck deck3 = createTestDeck();
        deck3.setCardsMastered(15);
        deck3.setTimesStudied(8);
        
        List<FlashcardDeck> decks = Arrays.asList(deck1, deck2, deck3);
        
        when(deckRepository.countByOwnerIdAndIsActiveTrue(userId)).thenReturn(3L);
        when(deckRepository.countTotalCardsByOwner(userId)).thenReturn(150L);
        when(deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId))
                .thenReturn(decks);

        // Act
        FlashcardDeckService.DeckGlobalStats result = flashcardDeckService.getGlobalStats(userId);

        // Assert
        assertEquals(3L, result.getTotalDecks());
        assertEquals(150L, result.getTotalCards());
        assertEquals(45L, result.getTotalMastered()); // 10 + 20 + 15
        assertEquals(23L, result.getTotalStudySessions()); // 5 + 10 + 8
    }
}
