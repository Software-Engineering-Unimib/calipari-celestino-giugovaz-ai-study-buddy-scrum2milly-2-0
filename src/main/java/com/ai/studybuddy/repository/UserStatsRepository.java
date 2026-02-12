package com.ai.studybuddy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ai.studybuddy.model.gamification.UserStats;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {

    Optional<UserStats> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    // Leaderboard per XP totale
    @Query("SELECT us FROM UserStats us ORDER BY us.totalXp DESC LIMIT :limit")
    List<UserStats> findTopByTotalXp(int limit);

    // Leaderboard per XP settimanale
    @Query("SELECT us FROM UserStats us ORDER BY us.weeklyXp DESC LIMIT :limit")
    List<UserStats> findTopByWeeklyXp(int limit);

    // Leaderboard per streak
    @Query("SELECT us FROM UserStats us ORDER BY us.currentStreak DESC LIMIT :limit")
    List<UserStats> findTopByStreak(int limit);

    // Leaderboard per livello
    @Query("SELECT us FROM UserStats us ORDER BY us.level DESC, us.totalXp DESC LIMIT :limit")
    List<UserStats> findTopByLevel(int limit);

    // Reset settimanale XP (da schedulare)
    @Modifying
    @Query("UPDATE UserStats us SET us.weeklyXp = 0")
    void resetWeeklyXp();

    // Reset mensile XP (da schedulare)
    @Modifying
    @Query("UPDATE UserStats us SET us.monthlyXp = 0")
    void resetMonthlyXp();


    /* DA SISTEMARE CURRENT_DATE - 1 */
    /*                               */
    // Trova utenti con streak da resettare (non hanno fatto attività ieri)
    @Query("SELECT us FROM UserStats us WHERE us.lastActivityDate < CURRENT_DATE AND us.currentStreak > 0")
    List<UserStats> findUsersWithBrokenStreak();
}