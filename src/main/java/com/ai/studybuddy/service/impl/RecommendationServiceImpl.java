package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.recommendation.RecommendationResponse;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.recommendation.Recommendation;
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

    // Iniezione tramite costruttore
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


    /*
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

    */



    @Override
    @Transactional
    public List<Recommendation> generateRecommendations(User user) {
        List<Recommendation> newRecs = new ArrayList<>();
        UserStats stats = gamificationService.getOrCreateUserStats(user.getId());
        UUID userId = user.getId();

        try {
            // ==================== 1. STREAK REMINDER ====================
            if (stats.getCurrentStreak() > 0 &&
                    stats.getLastActivityDate() != null &&
                    stats.getLastActivityDate().isBefore(java.time.LocalDate.now())) {

                Recommendation streakRec = createRecommendation(
                        user,
                        Recommendation.RecommendationType.STREAK_REMINDER,
                        "Mantieni il tuo streak! 🔥",
                        "Hai uno streak di " + stats.getCurrentStreak() + " giorni. Non perderlo!",
                        null,
                        "Non perdere il tuo streak di studio",
                        Recommendation.Priority.URGENT
                );
                if (streakRec != null) newRecs.add(streakRec);
            }

            // ==================== 2. ARGOMENTI DEBOLI (score < 60%) ====================
            List<UserProgress> weakTopics = userProgressRepository.findWeakTopics(userId, 60.0);
            for (UserProgress progress : weakTopics) {
                Recommendation weakTopicRec = createRecommendation(
                        user,
                        Recommendation.RecommendationType.WEAKNESS_FOCUS,
                        "Ripassa: " + progress.getTopic() + " 📚",
                        "Il tuo punteggio medio è " + Math.round(progress.getAverageScore()) + "%. Puoi migliorare!",
                        progress.getTopic(),
                        "Punteggio sotto il 60%",
                        Recommendation.Priority.HIGH
                );
                if (weakTopicRec != null) newRecs.add(weakTopicRec);
            }

            // ==================== 3. ARGOMENTI DA RIPASSARE (non studiati da 7+ giorni) ====================
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            List<UserProgress> topicsToReview = userProgressRepository.findTopicsNeedingReview(userId, cutoffDate);
            for (UserProgress progress : topicsToReview) {
                long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(
                        progress.getLastActivityAt().toLocalDate(),
                        java.time.LocalDate.now()
                );

                Recommendation reviewRec = createRecommendation(
                        user,
                        Recommendation.RecommendationType.REVIEW_TOPIC,
                        "Ripasso consigliato: " + progress.getTopic() + " 📖",
                        "Non studi questo argomento da " + daysAgo + " giorni",
                        progress.getTopic(),
                        "Il ripasso periodico migliora la memoria a lungo termine",
                        daysAgo > 14 ? Recommendation.Priority.HIGH : Recommendation.Priority.MEDIUM
                );
                if (reviewRec != null) newRecs.add(reviewRec);
            }

            // ==================== 4. ARGOMENTI RECENTI - CONTINUA A STUDIARE ====================
            List<UserProgress> recentTopics = userProgressRepository.findRecentTopics(userId, 3);
            for (UserProgress progress : recentTopics) {
                if (progress.getAverageScore() != null && progress.getAverageScore() >= 60 && progress.getAverageScore() < 80) {
                    Recommendation continueRec = createRecommendation(
                            user,
                            Recommendation.RecommendationType.REVIEW_TOPIC,
                            "Continua con: " + progress.getTopic() + " 🚀",
                            "Sei sulla buona strada! Punteggio attuale: " + Math.round(progress.getAverageScore()) + "%",
                            progress.getTopic(),
                            "Ancora un po' di pratica per padroneggiare l'argomento",
                            Recommendation.Priority.MEDIUM
                    );
                    if (continueRec != null) newRecs.add(continueRec);
                }
            }

            // ==================== 5. ARGOMENTI PIÙ STUDIATI - SFIDA TE STESSO ====================
            List<UserProgress> mostStudied = userProgressRepository.findMostStudiedTopics(userId);
            if (!mostStudied.isEmpty()) {
                UserProgress topTopic = mostStudied.get(0);
                if (topTopic.getAverageScore() != null && topTopic.getAverageScore() >= 80) {
                    Recommendation challengeRec = createRecommendation(
                            user,
                            Recommendation.RecommendationType.RETRY_QUIZ,
                            "Sfida te stesso: " + topTopic.getTopic() + " 🏆",
                            "Sei forte in questo argomento! Prova un quiz difficile.",
                            topTopic.getTopic(),
                            "Punteggio attuale: " + Math.round(topTopic.getAverageScore()) + "% - Punta al 100%!",
                            Recommendation.Priority.LOW
                    );
                    if (challengeRec != null) newRecs.add(challengeRec);
                }
            }

            // ==================== 6. STATISTICHE GENERALI ====================
            Double overallAverage = userProgressRepository.getOverallAverageScore(userId);
            Integer totalStudyMinutes = userProgressRepository.getTotalStudyMinutes(userId);
            Integer totalQuizCompleted = userProgressRepository.getTotalQuizCompleted(userId);
            long totalTopics = userProgressRepository.countByUserId(userId);

            // Suggerimento basato sulla media generale
            if (overallAverage != null && overallAverage < 70) {
                Recommendation improveRec = createRecommendation(
                        user,
                        Recommendation.RecommendationType.WEAKNESS_FOCUS,
                        "Migliora la tua media! 📈",
                        "La tua media generale è " + Math.round(overallAverage) + "%. Ripassa gli argomenti deboli.",
                        null,
                        "Una media sopra il 70% ti aiuterà a consolidare le conoscenze",
                        Recommendation.Priority.MEDIUM
                );
                if (improveRec != null) newRecs.add(improveRec);
            }

            // Suggerimento basato sul tempo di studio
            if (totalStudyMinutes == null || totalStudyMinutes < 60) {
                Recommendation studyMoreRec = createRecommendation(
                        user,
                        Recommendation.RecommendationType.DAILY_GOAL,
                        "Aumenta il tempo di studio ⏱️",
                        "Hai studiato meno di un'ora in totale. Prova a dedicare più tempo!",
                        null,
                        "Anche 15 minuti al giorno fanno la differenza",
                        Recommendation.Priority.MEDIUM
                );
                if (studyMoreRec != null) newRecs.add(studyMoreRec);
            }

            // Suggerimento per esplorare nuovi argomenti
            if (totalTopics < 5) {
                Recommendation exploreRec = createRecommendation(
                        user,
                        Recommendation.RecommendationType.NEW_TOPIC,
                        "Esplora nuovi argomenti! 🌟",
                        "Hai studiato solo " + totalTopics + " argomenti. Amplia i tuoi orizzonti!",
                        null,
                        "La varietà aiuta a mantenere alta la motivazione",
                        Recommendation.Priority.LOW
                );
                if (exploreRec != null) newRecs.add(exploreRec);
            }

        } catch (Exception e) {
            // Log dell'errore se hai un logger
            // logger.warn("Errore generazione raccomandazioni da UserProgress: {}", e.getMessage());
        }

        // ==================== 7. OBIETTIVO XP GIORNALIERO ====================
        if (stats.getWeeklyXp() == null || stats.getWeeklyXp() < 50) {
            Recommendation dailyGoal = createRecommendation(
                    user,
                    Recommendation.RecommendationType.DAILY_GOAL,
                    "Raggiungi 50 XP oggi! 🎯",
                    "Completa qualche attività per raggiungere il tuo obiettivo",
                    null,
                    "Guadagna XP per salire di livello",
                    Recommendation.Priority.MEDIUM
            );
            if (dailyGoal != null) newRecs.add(dailyGoal);
        }

        // ==================== 8. SUGGERISCI QUIZ SE MAI FATTO ====================
        if (stats.getQuizzesCompleted() == null || stats.getQuizzesCompleted() == 0) {
            Recommendation tryQuiz = createRecommendation(
                    user,
                    Recommendation.RecommendationType.NEW_TOPIC,
                    "Prova a creare un Quiz! 📝",
                    "Genera un quiz con l'AI per testare le tue conoscenze",
                    null,
                    "Non hai ancora completato nessun quiz",
                    Recommendation.Priority.MEDIUM
            );
            if (tryQuiz != null) newRecs.add(tryQuiz);
        }

        // ==================== 9. SUGGERISCI FLASHCARD SE MAI USATE ====================
        if (stats.getFlashcardsStudied() == null || stats.getFlashcardsStudied() == 0) {
            Recommendation tryFlashcards = createRecommendation(
                    user,
                    Recommendation.RecommendationType.STUDY_FLASHCARDS,
                    "Scopri le Flashcards! 🃏",
                    "Crea un deck di flashcards per memorizzare concetti",
                    null,
                    "Non hai ancora studiato nessuna flashcard",
                    Recommendation.Priority.MEDIUM
            );
            if (tryFlashcards != null) newRecs.add(tryFlashcards);
        }

        // ==================== 10. CONGRATULAZIONI PER TRAGUARDI ====================
        if (stats.getQuizzesCompleted() != null && stats.getQuizzesCompleted() >= 10) {
            Recommendation milestoneRec = createRecommendation(
                    user,
                    Recommendation.RecommendationType.DAILY_GOAL,
                    "Complimenti! 10+ Quiz completati 🎉",
                    "Stai facendo un ottimo lavoro! Continua così!",
                    null,
                    "Sei tra gli studenti più attivi",
                    Recommendation.Priority.LOW
            );
            if (milestoneRec != null) newRecs.add(milestoneRec);
        }

        // ==================== 11. BONUS WEEKEND ====================
        java.time.DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
        if (today == java.time.DayOfWeek.SATURDAY || today == java.time.DayOfWeek.SUNDAY) {
            Recommendation weekendRec = createRecommendation(
                    user,
                    Recommendation.RecommendationType.DAILY_GOAL,
                    "Studio del weekend! 📅",
                    "Approfitta del weekend per recuperare o approfondire",
                    null,
                    "Il weekend è perfetto per sessioni di studio più lunghe",
                    Recommendation.Priority.LOW
            );
            if (weekendRec != null) newRecs.add(weekendRec);
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
