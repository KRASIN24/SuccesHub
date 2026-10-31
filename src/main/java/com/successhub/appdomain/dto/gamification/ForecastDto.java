package com.successhub.appdomain.dto.gamification;

public record ForecastDto(
        int nextStreakMilestone,
        double nextStreakBonusRate,
        int weeklyChallengeMinDifficulty,
        int weeklyChallengeCount,
        int streakShields
) {}
