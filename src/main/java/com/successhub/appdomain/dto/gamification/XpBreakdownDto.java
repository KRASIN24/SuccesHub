package com.successhub.appdomain.dto.gamification;

public record XpBreakdownDto(
        int baseXp,
        int streakBonus,
        int firstTaskBonus,
        int variableBonus,
        int challengeBonus,
        int totalXp,
        int dailyXpRemaining
) {}
