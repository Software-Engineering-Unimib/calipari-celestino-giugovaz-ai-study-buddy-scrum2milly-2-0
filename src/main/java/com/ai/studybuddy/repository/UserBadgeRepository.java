package com.ai.studybuddy.repository;

import com.ai.studybuddy.model.gamification.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    List<UserBadge> findByUserIdOrderByUnlockedAtDesc(UUID userId);

    List<UserBadge> findByUserIdAndIsNewTrue(UUID userId);

    Optional<UserBadge> findByUserIdAndBadgeId(UUID userId, UUID badgeId);

    Optional<UserBadge> findByUserIdAndBadgeCode(UUID userId, String badgeCode);

    boolean existsByUserIdAndBadgeId(UUID userId, UUID badgeId);

    boolean existsByUserIdAndBadgeCode(UUID userId, String badgeCode);

    @Query("SELECT COUNT(ub) FROM UserBadge ub WHERE ub.user.id = :userId")
    long countByUserId(UUID userId);

    @Query("SELECT ub FROM UserBadge ub WHERE ub.user.id = :userId ORDER BY ub.unlockedAt DESC LIMIT :limit")
    List<UserBadge> findRecentByUserId(UUID userId, int limit);

    @Modifying
    @Query("UPDATE UserBadge ub SET ub.isNew = false WHERE ub.user.id = :userId")
    void markAllAsSeenForUser(UUID userId);
}