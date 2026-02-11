package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.recommendation.RecommendationResponse;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.model.user.UserProgress;
import com.ai.studybuddy.repository.RecommendationRepository;
import com.ai.studybuddy.repository.UserProgressRepository;
import com.ai.studybuddy.service.inter.GamificationService;
import com.ai.studybuddy.service.inter.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationServiceImpl implements RecommendationService {


    private final RecommendationRepository recommendationRepository;
    private final UserProgressRepository userProgressRepository;
    private final GamificationService gamificationService;

    // Iniezione tramite costruttore (rimuovi tutti gli @Autowired dai campi)
    public RecommendationServiceImpl(RecommendationRepository recommendationRepository,
                                     UserProgressRepository userProgressRepository,
                                     GamificationService gamificationService) {
        this.recommendationRepository = recommendationRepository;
        this.userProgressRepository = userProgressRepository;
        this.gamificationService = gamificationService;
    }

    @Override
    public List<RecommendationResponse> getActiveRecommendations(UUID userId) {
        List<com.ai.studybuddy.model.recommendation.Recommendation> active = recommendationRepository
                .findActiveByUserId(userId, LocalDateTime.now());
        return RecommendationResponse.fromList(active);
    }

    @Override
    @Transactional
    public List<com.ai.studybuddy.model.recommendation.Recommendation> generateRecommendations(User user) {
        List<com.ai.studybuddy.model.recommendation.Recommendation> newRecs = new ArrayList<>();
        UserStats stats = gamificationService.getOrCreateUserStats(user.getId());

        try {
            List<UserProgress> progressList = userProgressRepository.findByUserId(user.getId());

            // 1. Mantieni lo streak
            if (stats.getCurrentStreak() > 0 &&
                    stats.getLastActivityDate() != null &&
                    stats.getLastActivityDate().isBefore(java.time.LocalDate.now())) {

                com.ai.studybuddy.model.recommendation.Recommendation streakRec = createRecommendation(
                        user,
                        com.ai.studybuddy.model.recommendation.Recommendation.RecommendationType.STREAK_REMINDER,
                        "Mantieni il tuo streak! 🔥",
                        "Hai uno streak di " + stats.getCurrentStreak() + " giorni. Non perderlo!",
                        null,
                        "Non perdere il tuo streak di studio",
                        com.ai.studybuddy.model.recommendation.Recommendation.Priority.HIGH
                );
                if (streakRec != null) newRecs.add(streakRec);
            }

            // 2. Argomenti deboli (da UserProgress)
            for (UserProgress progress : progressList) {
                if (progress.getAverageScore() != null && progress.getAverageScore() < 60
                        && progress.getQuizCompleted() != null && progress.getQuizCompleted() > 0) {

                    com.ai.studybuddy.model.recommendation.Recommendation weakTopicRec = createRecommendation(
                            user,
                            com.ai.studybuddy.model.recommendation.Recommendation.RecommendationType.WEAKNESS_FOCUS,
                            "Ripassa: " + progress.getTopic() + " 📚",
                            "Il tuo punteggio medio è " + Math.round(progress.getAverageScore()) + "%. Prova a ripassare!",
                            progress.getTopic(),
                            "Punteggio sotto il 60%",
                            com.ai.studybuddy.model.recommendation.Recommendation.Priority.HIGH
                    );
                    if (weakTopicRec != null) newRecs.add(weakTopicRec);
                }
            }

            // 3. Argomenti non studiati da tempo
            for (UserProgress progress : progressList) {
                if (progress.getLastActivityAt() != null &&
                        progress.getLastActivityAt().isBefore(LocalDateTime.now().minusDays(7))) {

                    com.ai.studybuddy.model.recommendation.Recommendation reviewRec = createRecommendation(
                            user,
                            com.ai.studybuddy.model.recommendation.Recommendation.RecommendationType.REVIEW_TOPIC,
                            "Ripasso consigliato: " + progress.getTopic(),
                            "Non studi questo argomento da oltre una settimana",
                            progress.getTopic(),
                            "Ultima attività: " + progress.getLastActivityAt().toLocalDate(),
                            com.ai.studybuddy.model.recommendation.Recommendation.Priority.MEDIUM
                    );
                    if (reviewRec != null) newRecs.add(reviewRec);
                }
            }
        } catch (Exception e) {
            //logger.warn("Errore generazione raccomandazioni da UserProgress: {}", e.getMessage());
        }

        // 4. Obiettivo giornaliero XP
        if (stats.getWeeklyXp() == null || stats.getWeeklyXp() < 50) {
            com.ai.studybuddy.model.recommendation.Recommendation dailyGoal = createRecommendation(
                    user,
                    com.ai.studybuddy.model.recommendation.Recommendation.RecommendationType.DAILY_GOAL,
                    "Raggiungi 50 XP oggi! 🎯",
                    "Completa qualche attività per raggiungere il tuo obiettivo",
                    null,
                    "Guadagna XP per salire di livello",
                    com.ai.studybuddy.model.recommendation.Recommendation.Priority.MEDIUM
            );
            if (dailyGoal != null) newRecs.add(dailyGoal);
        }

        // 5. Suggerisci nuove funzionalità
        if (stats.getQuizzesCompleted() == null || stats.getQuizzesCompleted() == 0) {
            com.ai.studybuddy.model.recommendation.Recommendation tryQuiz = createRecommendation(
                    user,
                    com.ai.studybuddy.model.recommendation.Recommendation.RecommendationType.NEW_TOPIC,
                    "Prova a creare un Quiz! 📝",
                    "Genera un quiz con l'AI per testare le tue conoscenze",
                    null,
                    "Non hai ancora completato nessun quiz",
                    com.ai.studybuddy.model.recommendation.Recommendation.Priority.MEDIUM
            );
            if (tryQuiz != null) newRecs.add(tryQuiz);
        }

        if (stats.getFlashcardsStudied() == null || stats.getFlashcardsStudied() == 0) {
            com.ai.studybuddy.model.recommendation.Recommendation tryFlashcards = createRecommendation(
                    user,
                    com.ai.studybuddy.model.recommendation.Recommendation.RecommendationType.STUDY_FLASHCARDS,
                    "Scopri le Flashcards! 🃏",
                    "Crea un deck di flashcards per memorizzare concetti",
                    null,
                    "Non hai ancora studiato nessuna flashcard",
                    com.ai.studybuddy.model.recommendation.Recommendation.Priority.MEDIUM
            );
            if (tryFlashcards != null) newRecs.add(tryFlashcards);
        }

        return newRecs;
    }

    private com.ai.studybuddy.model.recommendation.Recommendation createRecommendation(User user, com.ai.studybuddy.model.recommendation.Recommendation.RecommendationType type,
                                                                                       String title, String description,
                                                                                       String topic, String reason, com.ai.studybuddy.model.recommendation.Recommendation.Priority priority) {
        // Verifica se esiste già una raccomandazione simile
        if (recommendationRepository.existsByUserIdAndTypeAndTopicAndIsDismissedFalseAndIsCompletedFalse(
                user.getId(), type, topic)) {
            return null;
        }

        com.ai.studybuddy.model.recommendation.Recommendation rec = new com.ai.studybuddy.model.recommendation.Recommendation();
        rec.setUser(user);
        rec.setType(type);
        rec.setTitle(title);
        rec.setDescription(description);
        rec.setTopic(topic);
        rec.setReason(reason);
        rec.setPriority(priority);
        rec.setExpiresAt(LocalDateTime.now().plusDays(1));

        return recommendationRepository.save(rec);
    }

    @Override
    @Transactional
    public void dismissRecommendation(UUID recommendationId, UUID userId) {
        com.ai.studybuddy.model.recommendation.Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Raccomandazione non trovata"));

        if (!rec.getUser().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        rec.dismiss();
        recommendationRepository.save(rec);
    }

    @Override
    @Transactional
    public void completeRecommendation(UUID recommendationId, UUID userId) {
        com.ai.studybuddy.model.recommendation.Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Raccomandazione non trovata"));

        if (!rec.getUser().getId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        rec.complete();
        recommendationRepository.save(rec);
    }
}
