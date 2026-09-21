package com.succeshub.appdomain.dto.gamification;

import com.succeshub.appdomain.dto.AchievementDto;
import com.succeshub.appdomain.dto.ProfileDto;
import com.succeshub.appdomain.dto.TaskDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class GamificationDto {

    private GamificationDto() {}

    public record XpBreakdownDto(
            int baseXp,
            int streakBonus,
            int firstTaskBonus,
            int variableBonus,
            int challengeBonus,
            int totalXp,
            int dailyXpRemaining
    ) {}

    public record XpPreviewDto(
            int baseXp,
            int streakBonus,
            int firstTaskBonus,
            int potentialVariableBonusMax,
            int challengeBonus,
            int estimatedTotalXp,
            int dailyXpRemaining
    ) {}

    public record BossDamageDto(
            UUID goalId,
            String goalName,
            double progressDelta,
            int healthRemaining,
            boolean goalCompleted,
            int goalXpReward
    ) {}

    public record RewardEventDto(
            XpBreakdownDto xp,
            boolean leveledUp,
            int previousLevel,
            int newLevel,
            boolean variableBonusTriggered,
            BossDamageDto bossDamage,
            List<AchievementDto> achievementsUnlocked,
            UUID lootBoxEarned
    ) {}

    public record TaskCompletionDto(
            TaskDto.Response task,
            RewardEventDto reward,
            ProfileDto updatedProfile
    ) {}

    public record DailyStatusDto(
            int missionsToday,
            int xpEarnedToday,
            int dailyXpRemaining,
            int currentStreak,
            String streakTier,
            int streakShields,
            /**
             * True when today qualifies and the user has not yet pressed Celebrate.
             * Distinct from streak settlement — {@code GET /daily} already lazy-closes past days.
             */
            boolean pendingCelebrations,
            /** True when today qualifies and Celebrate has already been acknowledged. */
            boolean daySealed,
            List<TaskDto.Response> weeklyChallenges,
            List<TaskDto.Response> scheduledToday
    ) {}

    public record CloseDayResultDto(
            int streakBefore,
            int streakAfter,
            String streakTier,
            List<AchievementDto> achievementsUnlocked,
            List<UUID> lootBoxesEarned,
            boolean alreadyClosed
    ) {}

    public record ForecastDto(
            int nextStreakMilestone,
            double nextStreakBonusRate,
            int weeklyChallengeMinDifficulty,
            int weeklyChallengeCount,
            int streakShields
    ) {}

    public record WeeklyInsightDto(
            int tasksThisWeek,
            int tasksLastWeek,
            int hardTasksThisWeek,
            int hardTasksLastWeek,
            int xpThisWeek,
            int xpLastWeek,
            double hardTaskDeltaPercent,
            String summaryText
    ) {}

    public record RewardItemDto(
            UUID id,
            String key,
            String label,
            String type,
            String rarity,
            String icon,
            String effect
    ) {}

    public record LootBoxDto(
            UUID id,
            String source,
            String boxType,
            String status,
            Instant createdAt,
            Instant openedAt,
            List<RewardItemDto> contents
    ) {}

    public record InventoryItemDto(
            UUID id,
            RewardItemDto reward,
            int quantity,
            boolean equipped
    ) {}

    /** A selectable box type with its display metadata and rarity odds (percentages). */
    public record BoxTypeDto(
            String id,
            String name,
            String source,
            String feel,
            String blurb,
            String icon,
            int commonWeight,
            int rareWeight,
            int legendaryWeight
    ) {}

    /** Request body for manually granting (summoning) a pending box of a given type. */
    public record GrantBoxRequest(String boxType) {}

    /** Client-safe gamification feature flags for the SPA. */
    public record ClientConfigDto(boolean enableLootDevGrants) {}

    public record ScheduleTasksRequest(List<UUID> taskIds) {}

    /**
     * A single calendar day within a streak calendar month.
     *
     * @param date   the calendar day
     * @param status one of {@code COMPLETED}, {@code MISSED}, {@code SHIELDED}, {@code TODAY}, {@code FUTURE}
     * @param completedTasks number of tasks completed on that day
     */
    public record StreakDayDto(LocalDate date, String status, int completedTasks) {}

    /**
     * A month of streak activity plus live streak headline figures.
     *
     * @param month             first day of the rendered month
     * @param currentStreak     active consecutive-day streak
     * @param streakTier        current tier ({@code NONE}/{@code BRONZE}/{@code SILVER}/{@code GOLD})
     * @param streakShields     shields banked on the profile (auto-protection)
     * @param shieldsAvailable  total shields the user can spend (banked + inventory stacks)
     * @param minTasksPerDay    tasks required for a day to qualify
     * @param days              per-day statuses for the month
     */
    public record StreakCalendarDto(
            LocalDate month,
            int currentStreak,
            String streakTier,
            int streakShields,
            int shieldsAvailable,
            int minTasksPerDay,
            List<StreakDayDto> days
    ) {}

    /** Result of a streak mutation (manual adjust, reset, or shield application). */
    public record StreakActionResultDto(
            int currentStreak,
            String streakTier,
            int streakShields,
            int shieldsAvailable,
            String message
    ) {}

    /** Request to change the streak by a signed delta (dev/testing tool). */
    public record AdjustStreakRequest(int delta) {}

    /** Request to set the streak to an absolute non-negative value (dev/testing tool). */
    public record SetStreakRequest(int value) {}

    /** Request to protect a specific missed day by spending a streak shield. */
    public record ShieldDayRequest(LocalDate date) {}

    /** Result of consuming (using) a utility inventory card. */
    public record UseItemResultDto(
            String rewardKey,
            String message,
            int quantityRemaining,
            ProfileDto updatedProfile
    ) {}
}
