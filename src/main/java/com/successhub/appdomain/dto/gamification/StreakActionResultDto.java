package com.successhub.appdomain.dto.gamification;

/** Result of a streak mutation (manual adjust, reset, or shield application). */
public record StreakActionResultDto(
        int currentStreak,
        String streakTier,
        int streakShields,
        int shieldsAvailable,
        String message
) {}
