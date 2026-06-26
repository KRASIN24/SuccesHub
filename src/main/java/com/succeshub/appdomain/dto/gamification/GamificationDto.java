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
            boolean pendingCelebrations,
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

    public record ScheduleTasksRequest(List<UUID> taskIds) {}
}
