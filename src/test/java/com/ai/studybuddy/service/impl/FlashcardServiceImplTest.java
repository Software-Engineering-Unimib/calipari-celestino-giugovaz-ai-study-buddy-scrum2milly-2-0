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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Captor
    private ArgumentCaptor<Flashcard> flashcardCaptor;

    private UUID userId;
    private UUID deckId;
    private UUID flashcardId;
    private User user;
    private FlashcardDeck deck;
    private Flashcard flashcard;
    private FlashcardCreateRequest createRequest;
    private FlashcardAIGenerateRequest aiGenerateRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        deckId = UUID.randomUUID();
        flashcardId = UUID.randomUUID();

        // Setup User
        user = new User();
        user.setId(userId);
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");

        // Setup FlashcardDeck
        deck = new FlashcardDeck();
        deck.setId(deckId);
        deck.setName("Test Deck");
        deck.setOwner(user);
        deck.setTotalCards(10);
        deck.setIsActive(true);

        // Setup Flashcard
        flashcard = new Flashcard();
        flashcard.setId(flashcardId);
        flashcard.setFrontContent("Front content");
        flashcard.setBackContent("Back content");
        flashcard.setDeck(deck);
        flashcard.setCreatedBy(user);
        flashcard.setIsActive(true);
        flashcard.setTimesReviewed(5);
        flashcard.setTimesCorrect(3);
        flashcard.setLastReviewedAt(LocalDateTime.now().minusDays(2));
        flashcard.setDifficultyLevel(DifficultyLevel.INTERMEDIO);
        flashcard.setAiGenerated(false);
        flashcard.setTags("tag1,tag2");

        // Setup CreateRequest
        createRequest = new FlashcardCreateRequest();
        createRequest.setFrontContent("New front");
        createRequest.setBackContent("New back");
        createRequest.setDifficultyLevel(DifficultyLevel.INTERMEDIO);

        // Setup AI GenerateRequest
        aiGenerateRequest = FlashcardAIGenerateRequest.builder()
                .topic("Mathematics")
                .numberOfCards(3)
                .difficultyLevel(DifficultyLevel.INTERMEDIO)
                .build();
    }

    @Test
    void createFlashcard_Success() {
        // Arrange
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(flashcardMapper.toEntity(createRequest, deck, user)).thenReturn(flashcard);
        when(flashcardRepository.save(flashcard)).thenReturn(flashcard);
        when(deckRepository.save(deck)).thenReturn(deck);

        // Act
        Flashcard result = flashcardService.createFlashcard(deckId, createRequest, user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(flashcardId);

        verify(deckRepository).findById(deckId);
        verify(flashcardMapper).toEntity(createRequest, deck, user);
        verify(flashcardRepository).save(flashcard);
        verify(deckRepository).save(deck);
        assertThat(deck.getTotalCards()).isEqualTo(11);
    }

    @Test
    void createFlashcard_DeckNotFound_ThrowsException() {
        // Arrange
        when(deckRepository.findById(deckId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> flashcardService.createFlashcard(deckId, createRequest, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Deck");

        verify(deckRepository).findById(deckId);
        verifyNoInteractions(flashcardMapper, flashcardRepository);
    }

    @Test
    void createFlashcard_UnauthorizedUser_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        deck.setOwner(otherUser);

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));

        // Act & Assert
        assertThatThrownBy(() -> flashcardService.createFlashcard(deckId, createRequest, user))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("deck");

        verify(deckRepository).findById(deckId);
        verifyNoInteractions(flashcardMapper, flashcardRepository);
    }

    @Test
    void generateAndSaveFlashcards_DeprecatedMethod_Success() {
        // Arrange
        String topic = "Mathematics";
        int numberOfCards = 3;
        String difficulty = "MEDIUM";

        List<Flashcard> expectedCards = Arrays.asList(flashcard);
        when(selfProxy.generateAndSaveFlashcards(eq(deckId), any(FlashcardAIGenerateRequest.class), eq(user)))
                .thenReturn(expectedCards);

        // Act
        List<Flashcard> result = flashcardService.generateAndSaveFlashcards(deckId, topic, numberOfCards, difficulty, user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(selfProxy).generateAndSaveFlashcards(eq(deckId), any(FlashcardAIGenerateRequest.class), eq(user));
    }

    @Test
    void generateAndSaveFlashcards_WithRequest_Success() {
        // Arrange
        String aiResponse = "{\"flashcards\": [...]}";
        JsonArray flashcardsJson = new JsonArray();

        JsonObject card1 = new JsonObject();
        card1.addProperty("front", "Front 1");
        card1.addProperty("back", "Back 1");
        flashcardsJson.add(card1);

        JsonObject card2 = new JsonObject();
        card2.addProperty("front", "Front 2");
        card2.addProperty("back", "Back 2");
        flashcardsJson.add(card2);

        Flashcard flashcard1 = new Flashcard();
        flashcard1.setId(UUID.randomUUID());
        Flashcard flashcard2 = new Flashcard();
        flashcard2.setId(UUID.randomUUID());

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(aiService.generateFlashcards(aiGenerateRequest.getTopic(),
                aiGenerateRequest.getNumberOfCards(),
                aiGenerateRequest.getDifficultyLevel()))
                .thenReturn(aiResponse);
        when(aiService.parseFlashcardsResponse(aiResponse)).thenReturn(flashcardsJson);
        when(flashcardMapper.toAIGeneratedEntity(any(FlashcardCreateRequest.class), eq(deck), eq(user)))
                .thenReturn(flashcard1, flashcard2);
        when(flashcardRepository.save(any(Flashcard.class)))
                .thenReturn(flashcard1, flashcard2);
        when(deckRepository.save(deck)).thenReturn(deck);

        // Act
        List<Flashcard> result = flashcardService.generateAndSaveFlashcards(deckId, aiGenerateRequest, user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(deckRepository).findById(deckId);
        verify(aiService).generateFlashcards(anyString(), anyInt(), any(DifficultyLevel.class));
        verify(aiService).parseFlashcardsResponse(aiResponse);
        verify(flashcardMapper, times(2)).toAIGeneratedEntity(any(FlashcardCreateRequest.class), eq(deck), eq(user));
        verify(flashcardRepository, times(2)).save(any(Flashcard.class));
        verify(deckRepository).save(deck);
        assertThat(deck.getTotalCards()).isEqualTo(12); // 10 + 2
    }

    @Test
    void getFlashcardsByDeck_Success() {
        // Arrange
        List<Flashcard> expectedCards = Arrays.asList(flashcard);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByDeckIdAndIsActiveTrue(deckId)).thenReturn(expectedCards);

        // Act
        List<Flashcard> result = flashcardService.getFlashcardsByDeck(deckId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(flashcard);

        verify(deckRepository).findById(deckId);
        verify(flashcardRepository).findByDeckIdAndIsActiveTrue(deckId);
    }

    @Test
    void reviewFlashcard_Correct_Success() {
        // Arrange
        int initialTimesReviewed = flashcard.getTimesReviewed();
        int initialTimesCorrect = flashcard.getTimesCorrect();

        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.save(flashcard)).thenReturn(flashcard);

        // Act
        Flashcard result = flashcardService.reviewFlashcard(flashcardId, true, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(flashcard.getTimesReviewed()).isEqualTo(initialTimesReviewed + 1);
        assertThat(flashcard.getTimesCorrect()).isEqualTo(initialTimesCorrect + 1);
        assertThat(flashcard.getLastReviewedAt()).isNotNull();
        verify(flashcardRepository).save(flashcard);
    }

    @Test
    void reviewFlashcard_Incorrect_Success() {
        // Arrange
        int initialTimesReviewed = flashcard.getTimesReviewed();
        int initialTimesCorrect = flashcard.getTimesCorrect();

        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.save(flashcard)).thenReturn(flashcard);

        // Act
        Flashcard result = flashcardService.reviewFlashcard(flashcardId, false, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(flashcard.getTimesReviewed()).isEqualTo(initialTimesReviewed + 1);
        assertThat(flashcard.getTimesCorrect()).isEqualTo(initialTimesCorrect);
        assertThat(flashcard.getLastReviewedAt()).isNotNull();
        verify(flashcardRepository).save(flashcard);
    }

    @Test
    void reviewFlashcard_Unauthorized_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        deck.setOwner(otherUser);

        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(flashcard));

        // Act & Assert
        assertThatThrownBy(() -> flashcardService.reviewFlashcard(flashcardId, true, userId))
                .isInstanceOf(UnauthorizedException.class);

        verify(flashcardRepository).findById(flashcardId);
        verify(flashcardRepository, never()).save(any());
    }

    @Test
    void updateFlashcard_Success() {
        // Arrange
        FlashcardCreateRequest updateRequest = new FlashcardCreateRequest();
        updateRequest.setFrontContent("Updated front");
        updateRequest.setBackContent("Updated back");
        updateRequest.setDifficultyLevel(DifficultyLevel.AVANZATO);

        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.save(flashcard)).thenReturn(flashcard);

        // Act
        Flashcard result = flashcardService.updateFlashcard(flashcardId, updateRequest, userId);

        // Assert
        assertThat(result).isNotNull();
        verify(flashcardMapper).updateEntity(flashcard, updateRequest);
        verify(flashcardRepository).save(flashcard);
    }

    @Test
    void deleteFlashcard_Success() {
        // Arrange
        int initialTotalCards = deck.getTotalCards();

        when(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.save(flashcard)).thenReturn(flashcard);
        when(deckRepository.save(deck)).thenReturn(deck);

        // Act
        flashcardService.deleteFlashcard(flashcardId, userId);

        // Assert
        assertThat(flashcard.getIsActive()).isFalse();
        verify(flashcardRepository).findById(flashcardId);
        verify(flashcardRepository).save(flashcard);
        verify(deckRepository).save(deck);
        assertThat(deck.getTotalCards()).isEqualTo(initialTotalCards - 1);
    }

    @Test
    void getStudySession_WithNeverReviewedCards() {
        // Arrange
        int numberOfCards = 3;
        List<Flashcard> neverReviewed = Arrays.asList(
                createFlashcardWithReviewCount(0),
                createFlashcardWithReviewCount(0),
                createFlashcardWithReviewCount(0),
                createFlashcardWithReviewCount(0)
        );

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findNeverReviewedByDeckId(deckId)).thenReturn(neverReviewed);

        // Act
        List<Flashcard> result = flashcardService.getStudySession(deckId, numberOfCards, userId);

        // Assert
        assertThat(result).hasSize(3);
        verify(flashcardRepository).findNeverReviewedByDeckId(deckId);
        verify(flashcardRepository, never()).findNeedingReview(any(), any());
        verify(flashcardRepository, never()).findRandomForStudy(any(), anyInt());
    }

    @Test
    void getStudySession_WithNeedReviewCards() {
        // Arrange
        int numberOfCards = 3;
        List<Flashcard> neverReviewed = Arrays.asList(
                createFlashcardWithReviewCount(0)
        );

        List<Flashcard> needReview = Arrays.asList(
                createFlashcardWithLastReviewedAt(LocalDateTime.now().minusDays(10)),
                createFlashcardWithLastReviewedAt(LocalDateTime.now().minusDays(10)),
                createFlashcardWithLastReviewedAt(LocalDateTime.now().minusDays(10)),
                createFlashcardWithLastReviewedAt(LocalDateTime.now().minusDays(10))
        );

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findNeverReviewedByDeckId(deckId)).thenReturn(neverReviewed);
        when(flashcardRepository.findNeedingReview(eq(deckId), any(LocalDateTime.class)))
                .thenReturn(needReview);

        // Act
        List<Flashcard> result = flashcardService.getStudySession(deckId, numberOfCards, userId);

        // Assert
        assertThat(result).hasSize(3);
        verify(flashcardRepository).findNeverReviewedByDeckId(deckId);
        verify(flashcardRepository).findNeedingReview(eq(deckId), any(LocalDateTime.class));
        verify(flashcardRepository, never()).findRandomForStudy(any(), anyInt());
    }

    @Test
    void getStudySession_WithRandomCards() {
        // Arrange
        int numberOfCards = 3;
        List<Flashcard> neverReviewed = Arrays.asList();
        List<Flashcard> needReview = Arrays.asList();
        List<Flashcard> randomCards = Arrays.asList(
                createFlashcardWithReviewCount(2),
                createFlashcardWithReviewCount(2),
                createFlashcardWithReviewCount(2)
        );

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findNeverReviewedByDeckId(deckId)).thenReturn(neverReviewed);
        when(flashcardRepository.findNeedingReview(eq(deckId), any(LocalDateTime.class)))
                .thenReturn(needReview);
        when(flashcardRepository.findRandomForStudy(deckId, numberOfCards)).thenReturn(randomCards);

        // Act
        List<Flashcard> result = flashcardService.getStudySession(deckId, numberOfCards, userId);

        // Assert
        assertThat(result).hasSize(3);
        verify(flashcardRepository).findNeverReviewedByDeckId(deckId);
        verify(flashcardRepository).findNeedingReview(eq(deckId), any(LocalDateTime.class));
        verify(flashcardRepository).findRandomForStudy(deckId, numberOfCards);
    }

    @Test
    void searchFlashcards_Success() {
        // Arrange
        String searchTerm = "math";
        List<Flashcard> searchResults = Arrays.asList(flashcard);

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(flashcardRepository.searchByContent(deckId, searchTerm)).thenReturn(searchResults);

        // Act
        List<Flashcard> result = flashcardService.searchFlashcards(deckId, searchTerm, userId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(flashcard);
        verify(deckRepository).findById(deckId);
        verify(flashcardRepository).searchByContent(deckId, searchTerm);
    }

    @Test
    void getFlashcardStats_Success() {
        // Arrange
        Flashcard masteredCard = createFlashcardWithSuccessRate(85.0);
        Flashcard needReviewCard = createFlashcardWithLastReviewedAt(LocalDateTime.now().minusDays(10));
        Flashcard normalCard = createFlashcardWithSuccessRate(60.0);

        List<Flashcard> allCards = Arrays.asList(masteredCard, needReviewCard, normalCard);

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByDeckIdAndIsActiveTrue(deckId)).thenReturn(allCards);

        // Act
        FlashcardService.FlashcardStats stats = flashcardService.getFlashcardStats(deckId, userId);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getTotal()).isEqualTo(3);
        assertThat(stats.getMastered()).isEqualTo(1);
        assertThat(stats.getNeedReview()).isEqualTo(1);
    }

    @Test
    void getFlashcardStats_EmptyDeck_Success() {
        // Arrange
        List<Flashcard> emptyList = Arrays.asList();

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByDeckIdAndIsActiveTrue(deckId)).thenReturn(emptyList);

        // Act
        FlashcardService.FlashcardStats stats = flashcardService.getFlashcardStats(deckId, userId);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getTotal()).isZero();
        assertThat(stats.getMastered()).isZero();
        assertThat(stats.getNeedReview()).isZero();
    }

    @Test
    void flashcard_GetSuccessRate_ReturnsCorrectPercentage() {
        // Arrange
        Flashcard card = new Flashcard();
        card.setTimesReviewed(10);
        card.setTimesCorrect(7);

        // Act
        double successRate = card.getSuccessRate();

        // Assert
        assertThat(successRate).isEqualTo(70.0);
    }

    @Test
    void flashcard_GetSuccessRate_WithNoReviews_ReturnsZero() {
        // Arrange
        Flashcard card = new Flashcard();
        card.setTimesReviewed(0);
        card.setTimesCorrect(0);

        // Act
        double successRate = card.getSuccessRate();

        // Assert
        assertThat(successRate).isZero();
    }

    @Test
    void flashcard_IsMastered_ReturnsTrueWhenSuccessRateAbove80() {
        // Arrange
        Flashcard card = new Flashcard();
        card.setTimesReviewed(10);
        card.setTimesCorrect(8); // 80%

        // Act
        boolean mastered = card.isMastered();

        // Assert
        assertThat(mastered).isTrue();
    }

    @Test
    void flashcard_IsMastered_ReturnsFalseWhenSuccessRateBelow80() {
        // Arrange
        Flashcard card = new Flashcard();
        card.setTimesReviewed(10);
        card.setTimesCorrect(7); // 70%

        // Act
        boolean mastered = card.isMastered();

        // Assert
        assertThat(mastered).isFalse();
    }

    @Test
    void flashcard_NeedsReview_WithNoReviews_ReturnsTrue() {
        // Arrange
        Flashcard card = new Flashcard();
        card.setTimesReviewed(0);

        // Act
        boolean needsReview = card.needsReview(7);

        // Assert
        assertThat(needsReview).isTrue();
    }

    @Test
    void flashcard_NeedsReview_WithLastReviewedAfterThreshold_ReturnsFalse() {
        // Arrange
        Flashcard card = new Flashcard();
        card.setTimesReviewed(5);
        card.setLastReviewedAt(LocalDateTime.now().minusDays(3));

        // Act
        boolean needsReview = card.needsReview(7);

        // Assert
        assertThat(needsReview).isFalse();
    }

    @Test
    void flashcard_NeedsReview_WithLastReviewedBeforeThreshold_ReturnsTrue() {
        // Arrange
        Flashcard card = new Flashcard();
        card.setTimesReviewed(5);
        card.setLastReviewedAt(LocalDateTime.now().minusDays(10));

        // Act
        boolean needsReview = card.needsReview(7);

        // Assert
        assertThat(needsReview).isTrue();
    }

    @Test
    void flashcard_TagsManagement_Success() {
        // Arrange
        Flashcard card = new Flashcard();

        // Test setTagsFromArray
        String[] tags = {"math", "algebra", "equations"};
        card.setTagsFromArray(tags);
        assertThat(card.getTags()).isEqualTo("math,algebra,equations");

        // Test getTagsArray
        String[] retrievedTags = card.getTagsArray();
        assertThat(retrievedTags).containsExactly("math", "algebra", "equations");

        // Test addTag
        card.addTag("geometry");
        assertThat(card.getTags()).isEqualTo("math,algebra,equations,geometry");

        // Test hasTag
        assertThat(card.hasTag("algebra")).isTrue();
        assertThat(card.hasTag("physics")).isFalse();

        // Test addTag null
        card.addTag(null);
        assertThat(card.getTags()).isEqualTo("math,algebra,equations,geometry");

        // Test addTag blank
        card.addTag("  ");
        assertThat(card.getTags()).isEqualTo("math,algebra,equations,geometry");
    }

    // ==================== HELPER METHODS ====================

    private Flashcard createFlashcardWithReviewCount(int timesReviewed) {
        Flashcard card = new Flashcard();
        card.setId(UUID.randomUUID());
        card.setDeck(deck);
        card.setCreatedBy(user);
        card.setTimesReviewed(timesReviewed);
        if (timesReviewed > 0) {
            card.setLastReviewedAt(LocalDateTime.now().minusDays(1));
        }
        return card;
    }

    private Flashcard createFlashcardWithSuccessRate(double successRate) {
        Flashcard card = new Flashcard();
        card.setId(UUID.randomUUID());
        card.setDeck(deck);
        card.setCreatedBy(user);

        // Calcola timesReviewed e timesCorrect per ottenere la percentuale desiderata
        int total = 10;
        int correct = (int) Math.round(successRate * total / 100);

        card.setTimesReviewed(total);
        card.setTimesCorrect(correct);
        card.setLastReviewedAt(LocalDateTime.now().minusDays(1));
        return card;
    }

    private Flashcard createFlashcardWithLastReviewedAt(LocalDateTime lastReviewedAt) {
        Flashcard card = new Flashcard();
        card.setId(UUID.randomUUID());
        card.setDeck(deck);
        card.setCreatedBy(user);
        card.setTimesReviewed(5);
        card.setLastReviewedAt(lastReviewedAt);
        return card;
    }
}