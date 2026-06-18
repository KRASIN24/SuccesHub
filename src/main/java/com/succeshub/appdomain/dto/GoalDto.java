package com.succeshub.appdomain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class GoalDto {

    public record Response(
            UUID id,
            String name,
            String tier,
            String targetDescription,
            double targetValue,
            double currentProgress,
            int healthRemaining,
            int xpReward,
            String icon,
            String status,
            boolean featured,
            String slainLabel,
            Instant completedAt,
            Instant createdAt
    ) {}

    public record CreateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 100) String tier,
            @Size(max = 255) String targetDescription,
            @Min(1) double targetValue,
            double currentProgress,
            @Min(0) int xpReward,
            @Size(max = 50) String icon,
            boolean featured
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 100) String tier,
            @Size(max = 255) String targetDescription,
            @Min(1) double targetValue,
            double currentProgress,
            @Min(0) int xpReward,
            @Size(max = 50) String icon,
            String status,
            boolean featured
    ) {}

    public record SummaryResponse(
            long totalGoals,
            long completedGoals,
            int overallPercent
    ) {}
}
