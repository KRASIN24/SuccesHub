package com.successhub.appdomain.dto.gamification;

import java.util.UUID;

public record BossDamageDto(
        UUID goalId,
        String goalName,
        double progressDelta,
        int healthRemaining,
        boolean goalCompleted,
        int goalXpReward
) {}
