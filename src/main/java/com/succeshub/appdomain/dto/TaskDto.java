package com.succeshub.appdomain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TaskDto {

    public record Response(
            UUID id,
            UUID categoryId,
            String categoryName,
            UUID goalId,
            UUID parentTaskId,
            boolean starred,
            int sortOrder,
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
            Instant reminderAt,
            Instant completedAt,
            Instant createdAt
    ) {}

    public record CreateRequest(
            UUID categoryId,
            UUID goalId,
            UUID parentTaskId,
            Boolean starred,
            Integer sortOrder,
            @NotBlank @Size(max = 255) String title,
            String description,
            @Min(0) int xpReward,
            @Min(1) Integer difficulty,
            @Min(1) Integer durationMinutes,
            @Min(1) Integer priority,
            String metaLabel,
            String metaType,
            LocalDate dueDate,
            Instant reminderAt
    ) {}

    public record UpdateRequest(
            UUID categoryId,
            UUID goalId,
            UUID parentTaskId,
            Boolean clearParent,
            Boolean starred,
            Integer sortOrder,
            @NotBlank @Size(max = 255) String title,
            String description,
            @Min(0) int xpReward,
            @Min(1) Integer difficulty,
            @Min(1) Integer durationMinutes,
            @Min(1) Integer priority,
            String status,
            String metaLabel,
            String metaType,
            LocalDate dueDate,
            Instant reminderAt,
            Boolean clearReminder
    ) {}

    public record ScheduleRequest(List<UUID> taskIds) {}

    public record StarRequest(@NotNull Boolean starred) {}

    public record ReorderRequest(
            UUID categoryId,
            UUID parentTaskId,
            @NotEmpty List<UUID> orderedIds
    ) {}
}
