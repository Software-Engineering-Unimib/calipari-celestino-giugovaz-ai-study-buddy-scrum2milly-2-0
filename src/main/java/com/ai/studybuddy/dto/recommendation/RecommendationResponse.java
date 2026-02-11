package com.ai.studybuddy.dto.recommendation;

import com.ai.studybuddy.model.recommendation.Recommendation;
import com.ai.studybuddy.model.recommendation.Recommendation.Priority;
import com.ai.studybuddy.model.recommendation.Recommendation.RecommendationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RecommendationResponse {

    private UUID id;
    private RecommendationType type;
    private String title;
    private String description;
    private String topic;
    private String reason;
    private Priority priority;
    private String actionUrl;
    private String actionType;
    private UUID relatedEntityId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    // Costruttore vuoto
    public RecommendationResponse() {}

    // Costruttore da entity
    public RecommendationResponse(Recommendation entity) {
        this.id = entity.getId();
        this.type = entity.getType();
        this.title = entity.getTitle();
        this.description = entity.getDescription();
        this.topic = entity.getTopic();
        this.reason = entity.getReason();
        this.priority = entity.getPriority();
        this.actionUrl = entity.getActionUrl();
        this.actionType = entity.getActionType();
        this.relatedEntityId = entity.getRelatedEntityId();
        this.expiresAt = entity.getExpiresAt();
        this.createdAt = entity.getCreatedAt();
    }

    // Factory method statico
    public static RecommendationResponse fromEntity(Recommendation entity) {
        return new RecommendationResponse(entity);
    }

    // Converte lista di entity in lista di DTO
    public static List<RecommendationResponse> fromList(List<Recommendation> entities) {
        return entities.stream()
                .map(RecommendationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==================== GETTERS E SETTERS ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RecommendationType getType() {
        return type;
    }

    public void setType(RecommendationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public UUID getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(UUID relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

















