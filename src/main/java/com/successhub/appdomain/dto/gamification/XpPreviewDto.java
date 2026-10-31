package com.successhub.appdomain.dto.gamification;

public record XpPreviewDto(
        int baseXp,
        int streakBonus,
        int firstTaskBonus,
        int potentialVariableBonusMax,
        int challengeBonus,
        int estimatedTotalXp,
        int dailyXpRemaining
) {}
