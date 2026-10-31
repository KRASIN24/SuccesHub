package com.successhub.appdomain.dto.gamification;

import java.util.UUID;

public record RewardItemDto(
        UUID id,
        String key,
        String label,
        String type,
        String rarity,
        String icon,
        String effect
) {}
