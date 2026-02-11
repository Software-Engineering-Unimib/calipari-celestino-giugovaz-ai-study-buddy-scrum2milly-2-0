package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.flashcard.FlashcardDeckCreateRequest;
import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.mapper.FlashcardMapper;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.FlashcardDeckRepository;
import com.ai.studybuddy.service.inter.FlashcardDeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashcardDeckServiceImplTest {

    @Mock
    private FlashcardDeckRepository deckRepository;

    @Mock
    private FlashcardMapper flashcardMapper;

    @InjectMocks
    private FlashcardDeckServiceImpl flashcardDeckService;

    @Captor
    private ArgumentCaptor<FlashcardDeck> deckCaptor;

    private UUID userId;
    private UUID deckId;
    private User owner;
    private FlashcardDeckCreateRequest createRequest;
    private FlashcardDeck flashcardDeck;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        deckId = UUID.randomUUID();

        // Crea User con setter
        owner = new User();
        owner.setId(userId);
        owner.setFirstName("Mario");
        owner.setLastName("Rossi");
        owner.setEmail("mario.rossi@example.com");
        owner.setPasswordHash("password123");

        // Crea FlashcardDeckCreateRequest
        createRequest = new FlashcardDeckCreateRequest();
        createRequest.setName("Test Deck");
        createRequest.setDescription("Test Description");
        createRequest.setSubject("Mathematics");
        createRequest.setIsPublic(true);

        // Crea FlashcardDeck con setter
        flashcardDeck = new FlashcardDeck();
        flashcardDeck.setId(deckId);
        flashcardDeck.setName("Test Deck");
        flashcardDeck.setDescription("Test Description");
        flashcardDeck.setSubject("Mathematics");
        flashcardDeck.setIsPublic(true);
        flashcardDeck.setIsActive(true);
        flashcardDeck.setOwner(owner);
        flashcardDeck.setTimesStudied(5);
        flashcardDeck.setCardsMastered(10);
    }

    @Test
    void createDeck_Success() {
        // Arrange
        when(flashcardMapper.toEntity(createRequest, owner)).thenReturn(flashcardDeck);
        when(deckRepository.save(flashcardDeck)).thenReturn(flashcardDeck);

        // Act
        FlashcardDeck result = flashcardDeckService.createDeck(createRequest, owner);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(deckId);
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        assertThat(result.getOwner()).isEqualTo(owner);

        verify(flashcardMapper).toEntity(createRequest, owner);
        verify(deckRepository).save(flashcardDeck);
        verifyNoMoreInteractions(flashcardMapper, deckRepository);
    }

    @Test
    void getUserDecks_Success() {
        // Arrange
        List<FlashcardDeck> expectedDecks = Arrays.asList(flashcardDeck);
        when(deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId))
                .thenReturn(expectedDecks);

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getUserDecks(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(flashcardDeck);

        verify(deckRepository).findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
        verifyNoMoreInteractions(deckRepository);
    }

    @Test
    void getDeck_Success() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(flashcardDeck));

        // Act
        FlashcardDeck result = flashcardDeckService.getDeck(deckId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(deckId);
        assertThat(result.getOwner().getId()).isEqualTo(userId);

        verify(deckRepository).findByIdAndOwnerIdAndIsActiveTrue(deckId, userId);
        verifyNoMoreInteractions(deckRepository);
    }

    @Test
    void getDeck_NotFound_ThrowsException() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> flashcardDeckService.getDeck(deckId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Deck")
                .hasMessageContaining(deckId.toString());

        verify(deckRepository).findByIdAndOwnerIdAndIsActiveTrue(deckId, userId);
        verifyNoMoreInteractions(deckRepository);
    }

    @Test
    void updateDeck_Success() {
        // Arrange
        FlashcardDeckCreateRequest updateRequest = new FlashcardDeckCreateRequest();
        updateRequest.setName("Updated Deck");
        updateRequest.setDescription("Updated Description");
        updateRequest.setSubject("Physics");
        updateRequest.setIsPublic(false);

        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(flashcardDeck));
        when(deckRepository.save(flashcardDeck)).thenReturn(flashcardDeck);

        // ACT & ASSERT - Risolviamo l'ambiguità specificando il tipo
        doAnswer(invocation -> {
            // Simula il comportamento del mapper
            FlashcardDeck deck = invocation.getArgument(0);
            FlashcardDeckCreateRequest req = invocation.getArgument(1);
            deck.setName(req.getName());
            deck.setDescription(req.getDescription());
            deck.setSubject(req.getSubject());
            deck.setIsPublic(req.getIsPublic());
            return null;
        }).when(flashcardMapper).updateEntity(any(FlashcardDeck.class), any(FlashcardDeckCreateRequest.class));

        // Act
        FlashcardDeck result = flashcardDeckService.updateDeck(deckId, updateRequest, userId);

        // Assert
        assertThat(result).isNotNull();
        verify(flashcardMapper).updateEntity(any(FlashcardDeck.class), any(FlashcardDeckCreateRequest.class));
        verify(deckRepository).findByIdAndOwnerIdAndIsActiveTrue(deckId, userId);
        verify(deckRepository).save(flashcardDeck);
    }

    @Test
    void updateDeck_DeckNotFound_ThrowsException() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> flashcardDeckService.updateDeck(deckId, createRequest, userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(deckRepository).findByIdAndOwnerIdAndIsActiveTrue(deckId, userId);
        verify(flashcardMapper, never()).updateEntity(any(FlashcardDeck.class), any(FlashcardDeckCreateRequest.class));
        verify(deckRepository, never()).save(any());
    }

    @Test
    void deleteDeck_Success() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(flashcardDeck));
        when(deckRepository.save(flashcardDeck)).thenReturn(flashcardDeck);

        // Act
        flashcardDeckService.deleteDeck(deckId, userId);

        // Assert
        assertThat(flashcardDeck.getIsActive()).isFalse();
        verify(deckRepository).findByIdAndOwnerIdAndIsActiveTrue(deckId, userId);
        verify(deckRepository).save(deckCaptor.capture());
        assertThat(deckCaptor.getValue().getIsActive()).isFalse();
    }

    @Test
    void deleteDeck_DeckNotFound_ThrowsException() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> flashcardDeckService.deleteDeck(deckId, userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(deckRepository).findByIdAndOwnerIdAndIsActiveTrue(deckId, userId);
        verify(deckRepository, never()).save(any());
    }

    @Test
    void recordStudySession_Success() {
        // Arrange
        int initialTimesStudied = flashcardDeck.getTimesStudied();

        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(flashcardDeck));
        when(deckRepository.save(flashcardDeck)).thenReturn(flashcardDeck);

        // Act
        flashcardDeckService.recordStudySession(deckId, userId);

        // Assert
        verify(deckRepository).save(deckCaptor.capture());
        FlashcardDeck capturedDeck = deckCaptor.getValue();
        assertThat(capturedDeck.getTimesStudied()).isEqualTo(initialTimesStudied + 1);
        verify(flashcardDeck).recordStudySession();
    }

    @Test
    void updateMasteredCount_Success() {
        // Arrange
        when(deckRepository.findByIdAndOwnerIdAndIsActiveTrue(deckId, userId))
                .thenReturn(Optional.of(flashcardDeck));
        when(deckRepository.save(flashcardDeck)).thenReturn(flashcardDeck);

        // Act
        flashcardDeckService.updateMasteredCount(deckId, userId);

        // Assert
        verify(flashcardDeck).updateMasteredCount();
        verify(deckRepository).save(flashcardDeck);
    }

    @Test
    void searchDecks_Success() {
        // Arrange
        String searchTerm = "math";
        List<FlashcardDeck> expectedDecks = Arrays.asList(flashcardDeck);

        when(deckRepository.searchByName(userId, searchTerm)).thenReturn(expectedDecks);

        // Act
        List<FlashcardDeck> result = flashcardDeckService.searchDecks(userId, searchTerm);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(flashcardDeck);
        verify(deckRepository).searchByName(userId, searchTerm);
    }

    @Test
    void getPublicDecks_Success() {
        // Arrange
        List<FlashcardDeck> publicDecks = Arrays.asList(flashcardDeck);
        when(deckRepository.findByIsPublicTrueAndIsActiveTrueOrderByTimesStudiedDesc())
                .thenReturn(publicDecks);

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getPublicDecks();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(flashcardDeck);
        verify(deckRepository).findByIsPublicTrueAndIsActiveTrueOrderByTimesStudiedDesc();
    }

    @Test
    void getDecksBySubject_Success() {
        // Arrange
        String subject = "Mathematics";
        List<FlashcardDeck> subjectDecks = Arrays.asList(flashcardDeck);

        when(deckRepository.findByOwnerIdAndSubjectAndIsActiveTrueOrderByNameAsc(userId, subject))
                .thenReturn(subjectDecks);

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getDecksBySubject(userId, subject);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(flashcardDeck);
        verify(deckRepository).findByOwnerIdAndSubjectAndIsActiveTrueOrderByNameAsc(userId, subject);
    }

    @Test
    void getDecksNeedingReview_Success() {
        // Arrange
        int daysAgo = 7;
        List<FlashcardDeck> decksNeedingReview = Arrays.asList(flashcardDeck);

        when(deckRepository.findNeedingReview(eq(userId), any(LocalDateTime.class)))
                .thenReturn(decksNeedingReview);

        // Act
        List<FlashcardDeck> result = flashcardDeckService.getDecksNeedingReview(userId, daysAgo);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(flashcardDeck);
        verify(deckRepository).findNeedingReview(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void getGlobalStats_Success() {
        // Arrange
        long totalDecks = 5L;
        long totalCards = 100L;

        FlashcardDeck deck1 = mock(FlashcardDeck.class);
        FlashcardDeck deck2 = mock(FlashcardDeck.class);

        when(deck1.getCardsMastered()).thenReturn(10);
        when(deck1.getTimesStudied()).thenReturn(5);
        when(deck2.getCardsMastered()).thenReturn(20);
        when(deck2.getTimesStudied()).thenReturn(3);

        List<FlashcardDeck> decks = Arrays.asList(deck1, deck2);

        when(deckRepository.countByOwnerIdAndIsActiveTrue(userId)).thenReturn(totalDecks);
        when(deckRepository.countTotalCardsByOwner(userId)).thenReturn(totalCards);
        when(deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)).thenReturn(decks);

        // Act
        FlashcardDeckService.DeckGlobalStats stats = flashcardDeckService.getGlobalStats(userId);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalDecks()).isEqualTo(totalDecks);
        assertThat(stats.getTotalCards()).isEqualTo(totalCards);
        assertThat(stats.getTotalMastered()).isEqualTo(30L);
        assertThat(stats.getTotalStudySessions()).isEqualTo(8L);
    }

    @Test
    void getGlobalStats_NoDecks_Success() {
        // Arrange
        long totalDecks = 0L;
        long totalCards = 0L;
        List<FlashcardDeck> emptyList = Arrays.asList();

        when(deckRepository.countByOwnerIdAndIsActiveTrue(userId)).thenReturn(totalDecks);
        when(deckRepository.countTotalCardsByOwner(userId)).thenReturn(totalCards);
        when(deckRepository.findByOwnerIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)).thenReturn(emptyList);

        // Act
        FlashcardDeckService.DeckGlobalStats stats = flashcardDeckService.getGlobalStats(userId);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalDecks()).isZero();
        assertThat(stats.getTotalCards()).isZero();
        assertThat(stats.getTotalMastered()).isZero();
        assertThat(stats.getTotalStudySessions()).isZero();
    }
}