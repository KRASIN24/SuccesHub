package com.succeshub.appdomain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class TaskDto {

    public record Response(
            UUID id,
            UUID categoryId,
            String categoryName,
            UUID goalId,
            String title,
            String description,
            int xpReward,
            int difficulty,
            int durationMinutes,
            int priority,
            boolean weeklyChallenge,
            LocalDate scheduledDate,
            String status,
            String metaLabel,
            String metaType,
            LocalDate dueDate,
            Instant completedAt,
            Instant createdAt
    ) {}

    public record CreateRequest(
            UUID categoryId,
            UUID goalId,
            @NotBlank @Size(max = 255) String title,
            String description,
            @Min(0) int xpReward,
            @Min(1) Integer difficulty,
            @Min(1) Integer durationMinutes,
            @Min(1) Integer priority,
            String metaLabel,
            String metaType,
            LocalDate dueDate
    ) {}

    public record UpdateRequest(
            UUID categoryId,
            UUID goalId,
            @NotBlank @Size(max = 255) String title,
            String description,
            @Min(0) int xpReward,
            @Min(1) Integer difficulty,
            @Min(1) Integer durationMinutes,
            @Min(1) Integer priority,
            String status,
            String metaLabel,
            String metaType,
            LocalDate dueDate
    ) {}

    public record ScheduleRequest(java.util.List<UUID> taskIds) {}
}
