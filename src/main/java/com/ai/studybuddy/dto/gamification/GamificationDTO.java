package com.ai.studybuddy.dto.gamification;

import com.ai.studybuddy.model.gamification.*;
import com.ai.studybuddy.model.gamification.UserStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO classes per Gamification API
 */
public class GamificationDTO {

    // ==================== XP EVENT RESPONSE ====================

    /**
     * Risposta dopo un evento che assegna XP
     */
    public static class XpEventResponse {
        private String eventType;
        private int xpEarned;
        private int newTotalXp;
        private int newLevel;
        private boolean leveledUp;
        private List<Badge> newBadges;

        public XpEventResponse(String eventType, int xpEarned, UserStats stats,
                               boolean leveledUp, List<Badge> newBadges) {
            this.eventType = eventType;
            this.xpEarned = xpEarned;
            this.newTotalXp = stats.getTotalXp();
            this.newLevel = stats.getLevel();
            this.leveledUp = leveledUp;
            this.newBadges = newBadges;
        }

        // Getters
        public String getEventType() { return eventType; }
        public int getXpEarned() { return xpEarned; }
        public int getNewTotalXp() { return newTotalXp; }
        public int getNewLevel() { return newLevel; }
        public boolean isLeveledUp() { return leveledUp; }
        public List<Badge> getNewBadges() { return newBadges; }
    }

    // ==================== USER STATS RESPONSE ====================

    /**
     * Risposta con statistiche complete dell'utente
     */
    public static class UserStatsResponse {
        private UUID userId;
        private int totalXp;
        private int weeklyXp;
        private int monthlyXp;
        private int level;
        private double levelProgress;
        private int xpForNextLevel;
        private int xpInCurrentLevel;

        private int currentStreak;
        private int longestStreak;

        private int explanationsRequested;
        private int quizzesCompleted;
        private int quizzesPassed;
        private int flashcardsStudied;
        private int focusSessionsCompleted;
        private int totalStudyTimeMinutes;

        private long badgesUnlocked;

        public static UserStatsResponse fromUserStats(UserStats stats, long badgeCount) {
            UserStatsResponse response = new UserStatsResponse();
            response.userId = stats.getUser().getId();
            response.totalXp = stats.getTotalXp() != null ? stats.getTotalXp() : 0;
            response.weeklyXp = stats.getWeeklyXp() != null ? stats.getWeeklyXp() : 0;
            response.monthlyXp = stats.getMonthlyXp() != null ? stats.getMonthlyXp() : 0;
            response.level = stats.getLevel() != null ? stats.getLevel() : 1;
            response.levelProgress = stats.getLevelProgressPercentage();
            response.xpForNextLevel = stats.getXpForNextLevel() != null ? stats.getXpForNextLevel() : 100;

            // Calcola XP nel livello corrente
            int xpForCurrentLevel = response.level > 1 ? (int)(100 * Math.pow(response.level, 1.5)) : 0;
            response.xpInCurrentLevel = response.totalXp - xpForCurrentLevel;

            response.currentStreak = stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0;
            response.longestStreak = stats.getLongestStreak() != null ? stats.getLongestStreak() : 0;

            response.explanationsRequested = stats.getExplanationsRequested() != null ? stats.getExplanationsRequested() : 0;
            response.quizzesCompleted = stats.getQuizzesCompleted() != null ? stats.getQuizzesCompleted() : 0;
            response.quizzesPassed = stats.getQuizzesPassed() != null ? stats.getQuizzesPassed() : 0;
            response.flashcardsStudied = stats.getFlashcardsStudied() != null ? stats.getFlashcardsStudied() : 0;
            response.focusSessionsCompleted = stats.getFocusSessionsCompleted() != null ? stats.getFocusSessionsCompleted() : 0;
            response.totalStudyTimeMinutes = stats.getTotalStudyTimeMinutes() != null ? stats.getTotalStudyTimeMinutes() : 0;

            response.badgesUnlocked = badgeCount;

            return response;
        }

        // Getters
        public UUID getUserId() { return userId; }
        public int getTotalXp() { return totalXp; }
        public int getWeeklyXp() { return weeklyXp; }
        public int getMonthlyXp() { return monthlyXp; }
        public int getLevel() { return level; }
        public double getLevelProgress() { return levelProgress; }
        public int getXpForNextLevel() { return xpForNextLevel; }
        public int getXpInCurrentLevel() { return xpInCurrentLevel; }
        public int getCurrentStreak() { return currentStreak; }
        public int getLongestStreak() { return longestStreak; }
        public int getExplanationsRequested() { return explanationsRequested; }
        public int getQuizzesCompleted() { return quizzesCompleted; }
        public int getQuizzesPassed() { return quizzesPassed; }
        public int getFlashcardsStudied() { return flashcardsStudied; }
        public int getFocusSessionsCompleted() { return focusSessionsCompleted; }
        public int getTotalStudyTimeMinutes() { return totalStudyTimeMinutes; }
        public long getBadgesUnlocked() { return badgesUnlocked; }
    }

    // ==================== BADGE RESPONSE ====================

    /**
     * Risposta badge con stato di sblocco e progresso
     */
    public static class BadgeResponse {
        private UUID id;
        private String name;
        private String description;
        private String icon;
        private String color;
        private String rarity;
        private Integer xpReward;
        private boolean unlocked;
        private LocalDateTime unlockedAt;
        private Double progress;

        public static BadgeResponse fromBadge(Badge badge, boolean unlocked,
                                              LocalDateTime unlockedAt, Double progress) {
            BadgeResponse response = new BadgeResponse();
            response.id = badge.getId();
            response.name = badge.getName();
            response.description = badge.getDescription();
            response.icon = badge.getIcon();
            response.color = badge.getColor();
            response.rarity = badge.getRarity() != null ? badge.getRarity().name() : "COMMON";
            response.xpReward = badge.getXpReward();
            response.unlocked = unlocked;
            response.unlockedAt = unlockedAt;
            response.progress = progress;
            return response;
        }

        public static BadgeResponse fromUserBadge(UserBadge userBadge) {
            return fromBadge(userBadge.getBadge(), true, userBadge.getUnlockedAt(), 100.0);
        }

        // Getters
        public UUID getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public String getColor() { return color; }
        public String getRarity() { return rarity; }
        public Integer getXpReward() { return xpReward; }
        public boolean isUnlocked() { return unlocked; }
        public LocalDateTime getUnlockedAt() { return unlockedAt; }
        public Double getProgress() { return progress; }
    }

    // ==================== RECOMMENDATION RESPONSE ====================

    /**
     * Risposta raccomandazione
     */
    public static class RecommendationResponse {
        private UUID id;
        private String type;
        private String title;
        private String description;
        private String topic;
        private String reason;
        private String priority;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        public static RecommendationResponse fromRecommendation(Recommendation rec) {
            RecommendationResponse response = new RecommendationResponse();
            response.id = rec.getId();
            response.type = rec.getType() != null ? rec.getType().name() : null;
            response.title = rec.getTitle();
            response.description = rec.getDescription();
            response.topic = rec.getTopic();
            response.reason = rec.getReason();
            response.priority = rec.getPriority() != null ? rec.getPriority().name() : "MEDIUM";
            response.createdAt = rec.getCreatedAt();
            response.expiresAt = rec.getExpiresAt();
            return response;
        }

        public static List<RecommendationResponse> fromList(List<Recommendation> recommendations) {
            return recommendations.stream()
                    .map(RecommendationResponse::fromRecommendation)
                    .collect(Collectors.toList());
        }

        // Getters
        public UUID getId() { return id; }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getTopic() { return topic; }
        public String getReason() { return reason; }
        public String getPriority() { return priority; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }

    // ==================== LEADERBOARD ENTRY ====================

    /**
     * Entry per la leaderboard
     */
    public static class LeaderboardEntry {
        private int rank;
        private UUID userId;
        private String userName;
        private String avatarUrl;
        private int value;
        private int level;
        private String type;

        public LeaderboardEntry(int rank, UserStats stats, String type) {
            this.rank = rank;
            this.userId = stats.getUser().getId();
            this.userName = stats.getUser().getFullName();
            this.avatarUrl = stats.getUser().getAvatarUrl();
            this.level = stats.getLevel() != null ? stats.getLevel() : 1;
            this.type = type;

            this.value = switch (type) {
                case "XP" -> stats.getTotalXp() != null ? stats.getTotalXp() : 0;
                case "WEEKLY_XP" -> stats.getWeeklyXp() != null ? stats.getWeeklyXp() : 0;
                case "STREAK" -> stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0;
                default -> stats.getTotalXp() != null ? stats.getTotalXp() : 0;
            };
        }

        // Getters
        public int getRank() { return rank; }
        public UUID getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getAvatarUrl() { return avatarUrl; }
        public int getValue() { return value; }
        public int getLevel() { return level; }
        public String getType() { return type; }

        // Setter for rank (to set after ordering)
        public void setRank(int rank) { this.rank = rank; }
    }
}