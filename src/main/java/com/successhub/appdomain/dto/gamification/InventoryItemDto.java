package com.successhub.appdomain.dto.gamification;

import java.util.UUID;

public record InventoryItemDto(
        UUID id,
        RewardItemDto reward,
        int quantity,
        boolean equipped
) {}
