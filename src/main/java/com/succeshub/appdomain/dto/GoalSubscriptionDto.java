package com.succeshub.appdomain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class GoalSubscriptionDto {

    public record Response(
            UUID id,
            UUID goalId,
            UUID categoryId,
            String categoryName,
            Instant createdAt
    ) {}

    public record SubscribeRequest(@NotNull UUID categoryId) {}
}
