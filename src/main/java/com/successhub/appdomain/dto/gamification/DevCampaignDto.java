package com.successhub.appdomain.dto.gamification;

import java.util.UUID;

/** Active campaign snapshot for Dev Campaigns tools. */
public record DevCampaignDto(
        UUID id,
        String name,
        double targetValue,
        double currentProgress,
        double pendingDamage,
        int healthRemaining,
        int subscriptionCount
) {}
