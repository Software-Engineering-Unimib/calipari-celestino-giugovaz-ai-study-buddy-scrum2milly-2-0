package com.ai.studybuddy.mapper;

import com.ai.studybuddy.dto.flashcard.FlashcardCreateRequest;
import com.ai.studybuddy.dto.flashcard.FlashcardDeckCreateRequest;
import com.ai.studybuddy.model.flashcard.Flashcard;
import com.ai.studybuddy.model.flashcard.FlashcardDeck;
import com.ai.studybuddy.model.user.User;
import org.springframework.stereotype.Component;

/**
 * Mapper per conversioni DTO <-> Entity
 */
@Component
public class FlashcardMapper {

    /**
     * Converte FlashcardCreateRequest in Flashcard entity
     */
    public Flashcard toEntity(FlashcardCreateRequest request, FlashcardDeck deck, User user) {
        Flashcard flashcard = new Flashcard();
        flashcard.setFrontContent(request.getFrontContent());
        flashcard.setBackContent(request.getBackContent());
        flashcard.setHint(request.getHint());
        flashcard.setDifficultyLevel(request.getDifficultyLevel());
        flashcard.setSource(request.getSource());
        flashcard.setDeck(deck);
        flashcard.setCreatedBy(user);

        if (request.getTags() != null) {
            flashcard.setTagsFromArray(request.getTags());
        }

        return flashcard;
    }

    /**
     * Converte FlashcardCreateRequest in Flashcard entity (AI generated)
     */
    public Flashcard toAIGeneratedEntity(FlashcardCreateRequest request, FlashcardDeck deck, User user) {
        Flashcard flashcard = toEntity(request, deck, user);
        flashcard.setAiGenerated(true);
        return flashcard;
    }

    /**
     * Aggiorna una Flashcard esistente con i dati del DTO
     */
    public void updateEntity(Flashcard flashcard, FlashcardCreateRequest request) {
        flashcard.setFrontContent(request.getFrontContent());
        flashcard.setBackContent(request.getBackContent());
        flashcard.setHint(request.getHint());
        flashcard.setDifficultyLevel(request.getDifficultyLevel());
        flashcard.setSource(request.getSource());

        if (request.getTags() != null) {
            flashcard.setTagsFromArray(request.getTags());
        }
    }

    /**
     * Converte FlashcardDeckCreateRequest in FlashcardDeck entity
     */
    public FlashcardDeck toEntity(FlashcardDeckCreateRequest request, User owner) {
        FlashcardDeck deck = new FlashcardDeck();
        deck.setName(request.getName());
        deck.setDescription(request.getDescription());
        deck.setSubject(request.getSubject());
        deck.setColor(request.getColor());
        deck.setIcon(request.getIcon());
        deck.setIsPublic(request.getIsPublic());
        deck.setOwner(owner);
        return deck;
    }

    /**
     * Aggiorna un FlashcardDeck esistente con i dati del DTO
     */
    public void updateEntity(FlashcardDeck deck, FlashcardDeckCreateRequest request) {
        deck.setName(request.getName());
        deck.setDescription(request.getDescription());
        deck.setSubject(request.getSubject());
        deck.setColor(request.getColor());
        deck.setIcon(request.getIcon());
        deck.setIsPublic(request.getIsPublic());
    }

    /**
     * Crea FlashcardCreateRequest da risposta AI JSON
     */
    public FlashcardCreateRequest fromAIResponse(String front, String back, String topic) {
        return FlashcardCreateRequest.builder()
                .frontContent(front)
                .backContent(back)
                .tags("ai-generated", topic)
                .build();
    }
}