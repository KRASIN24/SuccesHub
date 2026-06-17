package com.succeshub.appdomain.dto;

import java.time.Instant;
import java.util.UUID;

public record AchievementDto(
        UUID id,
        String key,
        String label,
        String icon,
        String description,
        boolean locked,
        Instant unlockedAt
) {}
