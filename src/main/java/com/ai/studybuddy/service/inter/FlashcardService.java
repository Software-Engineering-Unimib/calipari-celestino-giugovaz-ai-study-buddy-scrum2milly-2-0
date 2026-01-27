package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.dto.flashcard.FlashcardAIGenerateRequest;
import com.ai.studybuddy.dto.flashcard.FlashcardCreateRequest;
import com.ai.studybuddy.model.flashcard.Flashcard;
import com.ai.studybuddy.model.user.User;

import java.util.List;
import java.util.UUID;

/**
 * Interfaccia per il servizio Flashcard
 */
public interface FlashcardService {

    /**
     * Crea una nuova flashcard
     */
    Flashcard createFlashcard(UUID deckId, FlashcardCreateRequest request, User user);

    /**
     * Genera E salva flashcard usando AI (metodo legacy)
     * @deprecated Usa {@link #generateAndSaveFlashcards(UUID, FlashcardAIGenerateRequest, User)} invece
     */
    @Deprecated
    List<Flashcard> generateAndSaveFlashcards(UUID deckId, String topic, int numberOfCards,
                                              String difficulty, User user);

    /**
     * Genera E salva flashcard usando AI
     */
    List<Flashcard> generateAndSaveFlashcards(UUID deckId, FlashcardAIGenerateRequest request, User user);

    /**
     * Ottiene tutte le flashcard di un deck
     */
    List<Flashcard> getFlashcardsByDeck(UUID deckId, UUID userId);

    /**
     * Registra una revisione di una flashcard
     */
    Flashcard reviewFlashcard(UUID flashcardId, boolean wasCorrect, UUID userId);

    /**
     * Aggiorna una flashcard esistente
     */
    Flashcard updateFlashcard(UUID flashcardId, FlashcardCreateRequest request, UUID userId);

    /**
     * Elimina (soft delete) una flashcard
     */
    void deleteFlashcard(UUID flashcardId, UUID userId);

    /**
     * Ottiene flashcard casuali per una sessione di studio
     */
    List<Flashcard> getStudySession(UUID deckId, int numberOfCards, UUID userId);

    /**
     * Cerca flashcards per contenuto
     */
    List<Flashcard> searchFlashcards(UUID deckId, String searchTerm, UUID userId);

    /**
     * Ottiene statistiche delle flashcard
     */
    FlashcardStats getFlashcardStats(UUID deckId, UUID userId);

    /**
     * Classe per le statistiche
     */
    class FlashcardStats {
        private final long total;
        private final long mastered;
        private final long needReview;
        private final double masteryPercentage;

        public FlashcardStats(long total, long mastered, long needReview) {
            this.total = total;
            this.mastered = mastered;
            this.needReview = needReview;
            this.masteryPercentage = total > 0 ? (double) mastered / total * 100 : 0.0;
        }

        public long getTotal() { return total; }
        public long getMastered() { return mastered; }
        public long getNeedReview() { return needReview; }
        public double getMasteryPercentage() { return masteryPercentage; }
    }
}
