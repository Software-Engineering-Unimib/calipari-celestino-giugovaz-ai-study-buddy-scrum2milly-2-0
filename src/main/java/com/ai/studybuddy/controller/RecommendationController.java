package com.ai.studybuddy.controller;

import com.ai.studybuddy.dto.recommendation.RecommendationResponse;
import com.ai.studybuddy.model.recommendation.Recommendation;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.service.impl.RecommendationServiceImpl;
import com.ai.studybuddy.service.inter.RecommendationService;
import com.ai.studybuddy.service.inter.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Controller per la gestione delle raccomandazioni di studio
 *
 * Responsabilità:
 * - Ottenere raccomandazioni attive per l'utente
 * - Generare nuove raccomandazioni tramite AI
 * - Gestire lo stato delle raccomandazioni (completate/ignorate)
 */
@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
@SuppressWarnings("java:S5122")
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;
    private final UserService userService;

    public RecommendationController(RecommendationService recommendationService, UserService userService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    /**
     * Ottiene le raccomandazioni attive per l'utente corrente
     *
     * @param principal Utente autenticato
     * @return Lista delle raccomandazioni attive
     */
    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Richiesta raccomandazioni per utente: {}", user.getEmail());

        List<RecommendationResponse> recommendations = recommendationService.getActiveRecommendations(user.getId());

        logger.debug("Trovate {} raccomandazioni attive per {}", recommendations.size(), user.getEmail());
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Genera nuove raccomandazioni personalizzate usando AI
     *
     * @param principal Utente autenticato
     * @return Lista delle nuove raccomandazioni generate
     */
    @PostMapping("/generate")
    public ResponseEntity<List<RecommendationResponse>> generateRecommendations(Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Generazione nuove raccomandazioni per utente: {}", user.getEmail());

        List<Recommendation> newRecommendations = recommendationService.generateRecommendations(user);

        logger.info("Generate {} nuove raccomandazioni per {}", newRecommendations.size(), user.getEmail());
        return ResponseEntity.ok(RecommendationResponse.fromList(newRecommendations));
    }

    /**
     * Ignora una raccomandazione (non verrà più mostrata)
     *
     * @param id ID della raccomandazione
     * @param principal Utente autenticato
     * @return 200 OK se l'operazione ha successo
     */
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismissRecommendation(
            @PathVariable UUID id,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Dismissing raccomandazione {} per utente: {}", id, user.getEmail());

        recommendationService.dismissRecommendation(id, user.getId());

        return ResponseEntity.ok().build();
    }

    /**
     * Segna una raccomandazione come completata
     *
     * @param id ID della raccomandazione
     * @param principal Utente autenticato
     * @return 200 OK se l'operazione ha successo
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeRecommendation(
            @PathVariable UUID id,
            Principal principal) {
        User user = userService.getCurrentUser(principal);
        logger.info("Completamento raccomandazione {} per utente: {}", id, user.getEmail());

        recommendationService.completeRecommendation(id, user.getId());

        return ResponseEntity.ok().build();
    }
}