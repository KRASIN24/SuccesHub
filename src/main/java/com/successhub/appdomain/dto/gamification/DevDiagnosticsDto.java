package com.successhub.appdomain.dto.gamification;

/**
 * Live diagnostics snapshot for the Dev Bubble strip.
 *
 * @param effectiveToday         ISO date used as gamification today
 * @param dayOffset              virtual day offset
 * @param lastProcessedDay       last day settled by ritual close
 * @param lastCelebratedDay      last celebrate seal day
 * @param lastWeeklyLootWeek     Monday of last weekly vault grant week
 * @param level                  current level
 * @param currentXp              XP into current level
 * @param nextLevelXp            XP required for next level
 * @param lifetimeXp             lifetime XP total
 * @param dailyXpEarned          XP earned on the daily counter date
 * @param dailyXpRemaining       remaining daily XP cap
 * @param currentStreak          live streak
 * @param streakShields          banked shields
 * @param pendingDoubleStrike    Double Strike armed
 * @param pendingSurgeToken      Surge Token armed
 * @param pendingLootCount       unopened cache boxes
 * @param activeCampaignPending  sum of pendingDamage across active campaigns
 */
public record DevDiagnosticsDto(
        String effectiveToday,
        int dayOffset,
        String lastProcessedDay,
        String lastCelebratedDay,
        String lastWeeklyLootWeek,
        int level,
        int currentXp,
        int nextLevelXp,
        int lifetimeXp,
        int dailyXpEarned,
        int dailyXpRemaining,
        int currentStreak,
        int streakShields,
        boolean pendingDoubleStrike,
        boolean pendingSurgeToken,
        int pendingLootCount,
        double activeCampaignPending
) {}
