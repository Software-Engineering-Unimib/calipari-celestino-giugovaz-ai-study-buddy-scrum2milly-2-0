package com.ai.studybuddy.repository;

import com.ai.studybuddy.model.gamification.Badge;
import com.ai.studybuddy.model.gamification.Badge.BadgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    Optional<Badge> findByCode(String code);

    List<Badge> findByIsActiveTrueOrderByRequirementValueAsc();

    List<Badge> findByCategoryAndIsActiveTrueOrderByRequirementValueAsc(BadgeCategory category);

    @Query("SELECT b FROM Badge b WHERE b.isActive = true AND b.requirementType = :type AND b.requirementValue <= :value")
    List<Badge> findUnlockableBadges(String type, Integer value);

    @Query("SELECT b FROM Badge b WHERE b.isActive = true AND b.id NOT IN " +
            "(SELECT ub.badge.id FROM UserBadge ub WHERE ub.user.id = :userId)")
    List<Badge> findLockedBadgesForUser(UUID userId);

    boolean existsByCode(String code);
}