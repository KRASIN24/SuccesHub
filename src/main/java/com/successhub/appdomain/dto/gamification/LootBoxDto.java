package com.successhub.appdomain.dto.gamification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LootBoxDto(
        UUID id,
        String source,
        String boxType,
        String status,
        Instant createdAt,
        Instant openedAt,
        List<RewardItemDto> contents
) {}
