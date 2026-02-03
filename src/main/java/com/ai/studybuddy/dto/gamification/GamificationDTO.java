package com.ai.studybuddy.dto.gamification;

import com.ai.studybuddy.model.gamification.Badge;
import com.ai.studybuddy.model.gamification.UserBadge;
import com.ai.studybuddy.model.gamification.UserStats;
import com.ai.studybuddy.model.gamification.Recommendation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO per le risposte del sistema di gamification
 */
public class GamificationDTO {

    // ==================== BADGE DTOs ====================

    public static class BadgeResponse {
        private UUID id;
        private String code;
        private String name;
        private String description;
        private String icon;
        private String color;
        private String category;
        private String rarity;
        private Integer requirementValue;
        private Integer xpReward;
        private boolean unlocked;
        private LocalDateTime unlockedAt;
        private Double progress;  // Percentuale progresso (0-100)

        public static BadgeResponse fromBadge(Badge badge, boolean unlocked, LocalDateTime unlockedAt, Double progress) {
            BadgeResponse dto = new BadgeResponse();
            dto.id = badge.getId();
            dto.code = badge.getCode();
            dto.name = badge.getName();
            dto.description = badge.getDescription();
            dto.icon = badge.getIcon();
            dto.color = badge.getColor();
            dto.category = badge.getCategory() != null ? badge.getCategory().name() : null;
            dto.rarity = badge.getRarity() != null ? badge.getRarity().name() : null;
            dto.requirementValue = badge.getRequirementValue();
            dto.xpReward = badge.getXpReward();
            dto.unlocked = unlocked;
            dto.unlockedAt = unlockedAt;
            dto.progress = progress;
            return dto;
        }

        public static BadgeResponse fromUserBadge(UserBadge userBadge) {
            return fromBadge(userBadge.getBadge(), true, userBadge.getUnlockedAt(), 100.0);
        }

        // Getters
        public UUID getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public String getColor() { return color; }
        public String getCategory() { return category; }
        public String getRarity() { return rarity; }
        public Integer getRequirementValue() { return requirementValue; }
        public Integer getXpReward() { return xpReward; }
        public boolean isUnlocked() { return unlocked; }
        public LocalDateTime getUnlockedAt() { return unlockedAt; }
        public Double getProgress() { return progress; }
    }

    // ==================== STATS DTOs ====================

    public static class UserStatsResponse {
        private Integer totalXp;
        private Integer weeklyXp;
        private Integer monthlyXp;
        private Integer level;
        private Double levelProgress;
        private Integer xpForNextLevel;
        private Integer currentStreak;
        private Integer longestStreak;
        private Integer explanationsRequested;
        private Integer quizzesCompleted;
        private Integer quizzesPassed;
        private Integer flashcardsStudied;
        private Integer flashcardsMastered;
        private Integer focusSessionsCompleted;
        private Integer totalStudyTimeMinutes;
        private Integer badgesUnlocked;

        public static UserStatsResponse fromUserStats(UserStats stats, long badgeCount) {
            UserStatsResponse dto = new UserStatsResponse();
            dto.totalXp = stats.getTotalXp();
            dto.weeklyXp = stats.getWeeklyXp();
            dto.monthlyXp = stats.getMonthlyXp();
            dto.level = stats.getLevel();
            dto.levelProgress = stats.getLevelProgressPercentage();
            dto.xpForNextLevel = stats.getXpForNextLevel();
            dto.currentStreak = stats.getCurrentStreak();
            dto.longestStreak = stats.getLongestStreak();
            dto.explanationsRequested = stats.getExplanationsRequested();
            dto.quizzesCompleted = stats.getQuizzesCompleted();
            dto.quizzesPassed = stats.getQuizzesPassed();
            dto.flashcardsStudied = stats.getFlashcardsStudied();
            dto.flashcardsMastered = stats.getFlashcardsMastered();
            dto.focusSessionsCompleted = stats.getFocusSessionsCompleted();
            dto.totalStudyTimeMinutes = stats.getTotalStudyTimeMinutes();
            dto.badgesUnlocked = (int) badgeCount;
            return dto;
        }

        // Getters
        public Integer getTotalXp() { return totalXp; }
        public Integer getWeeklyXp() { return weeklyXp; }
        public Integer getMonthlyXp() { return monthlyXp; }
        public Integer getLevel() { return level; }
        public Double getLevelProgress() { return levelProgress; }
        public Integer getXpForNextLevel() { return xpForNextLevel; }
        public Integer getCurrentStreak() { return currentStreak; }
        public Integer getLongestStreak() { return longestStreak; }
        public Integer getExplanationsRequested() { return explanationsRequested; }
        public Integer getQuizzesCompleted() { return quizzesCompleted; }
        public Integer getQuizzesPassed() { return quizzesPassed; }
        public Integer getFlashcardsStudied() { return flashcardsStudied; }
        public Integer getFlashcardsMastered() { return flashcardsMastered; }
        public Integer getFocusSessionsCompleted() { return focusSessionsCompleted; }
        public Integer getTotalStudyTimeMinutes() { return totalStudyTimeMinutes; }
        public Integer getBadgesUnlocked() { return badgesUnlocked; }
    }

    // ==================== RECOMMENDATION DTOs ====================

    public static class RecommendationResponse {
        private UUID id;
        private String type;
        private String title;
        private String description;
        private String topic;
        private String reason;
        private String priority;
        private String actionUrl;
        private String actionType;
        private UUID relatedEntityId;
        private LocalDateTime createdAt;

        public static RecommendationResponse fromRecommendation(Recommendation rec) {
            RecommendationResponse dto = new RecommendationResponse();
            dto.id = rec.getId();
            dto.type = rec.getType().name();
            dto.title = rec.getTitle();
            dto.description = rec.getDescription();
            dto.topic = rec.getTopic();
            dto.reason = rec.getReason();
            dto.priority = rec.getPriority().name();
            dto.actionUrl = rec.getActionUrl();
            dto.actionType = rec.getActionType();
            dto.relatedEntityId = rec.getRelatedEntityId();
            dto.createdAt = rec.getCreatedAt();
            return dto;
        }

        public static List<RecommendationResponse> fromList(List<Recommendation> recs) {
            return recs.stream()
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
        public String getActionUrl() { return actionUrl; }
        public String getActionType() { return actionType; }
        public UUID getRelatedEntityId() { return relatedEntityId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    // ==================== XP EVENT DTOs ====================

    public static class XpEventResponse {
        private String event;
        private Integer xpEarned;
        private Integer totalXp;
        private Integer level;
        private boolean leveledUp;
        private List<BadgeResponse> newBadges;

        public XpEventResponse(String event, int xpEarned, UserStats stats,
                               boolean leveledUp, List<Badge> newBadges) {
            this.event = event;
            this.xpEarned = xpEarned;
            this.totalXp = stats.getTotalXp();
            this.level = stats.getLevel();
            this.leveledUp = leveledUp;
            this.newBadges = newBadges.stream()
                    .map(b -> BadgeResponse.fromBadge(b, true, LocalDateTime.now(), 100.0))
                    .collect(Collectors.toList());
        }

        // Getters
        public String getEvent() { return event; }
        public Integer getXpEarned() { return xpEarned; }
        public Integer getTotalXp() { return totalXp; }
        public Integer getLevel() { return level; }
        public boolean isLeveledUp() { return leveledUp; }
        public List<BadgeResponse> getNewBadges() { return newBadges; }
    }

    // ==================== LEADERBOARD DTOs ====================

    public static class LeaderboardEntry {
        private int rank;
        private UUID odificativUserId;
        private String userName;
        private String avatarUrl;
        private Integer value;
        private Integer level;

        public LeaderboardEntry(int rank, UserStats stats, String type) {
            this.rank = rank;
            this.odificativUserId = stats.getUser().getId();
            this.userName = stats.getUser().getFirstName() + " " +
                    (stats.getUser().getLastName() != null ?
                            stats.getUser().getLastName().charAt(0) + "." : "");
            this.avatarUrl = stats.getUser().getAvatarUrl();
            this.level = stats.getLevel();

            switch (type) {
                case "XP" -> this.value = stats.getTotalXp();
                case "WEEKLY_XP" -> this.value = stats.getWeeklyXp();
                case "STREAK" -> this.value = stats.getCurrentStreak();
                case "LEVEL" -> this.value = stats.getLevel();
                default -> this.value = 0;
            }
        }

        // Getters
        public int getRank() { return rank; }
        public UUID getUserId() { return odificativUserId; }
        public String getUserName() { return userName; }
        public String getAvatarUrl() { return avatarUrl; }
        public Integer getValue() { return value; }
        public Integer getLevel() { return level; }
    }
}