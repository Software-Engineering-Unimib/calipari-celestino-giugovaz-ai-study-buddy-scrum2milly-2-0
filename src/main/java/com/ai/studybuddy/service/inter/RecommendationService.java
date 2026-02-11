package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.dto.recommendation.RecommendationResponse;
import com.ai.studybuddy.model.user.User;

import java.util.List;
import java.util.UUID;

public interface RecommendationService {


    /**
     * Ottiene le raccomandazioni attive per l'utente
     */
    List<RecommendationResponse> getActiveRecommendations(UUID userId);

    /**
     * Genera nuove raccomandazioni basate sui progressi
     */
    List<com.ai.studybuddy.model.recommendation.Recommendation> generateRecommendations(User user);

    /**
     * Ignora una raccomandazione
     */
    void dismissRecommendation(UUID recommendationId, UUID userId);

    /**
     * Segna una raccomandazione come completata
     */
    void completeRecommendation(UUID recommendationId, UUID userId);

}
