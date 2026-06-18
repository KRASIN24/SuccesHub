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
            String title,
            String description,
            int xpReward,
            String status,
            String metaLabel,
            String metaType,
            LocalDate dueDate,
            Instant completedAt,
            Instant createdAt
    ) {}

    public record CreateRequest(
            UUID categoryId,
            @NotBlank @Size(max = 255) String title,
            String description,
            @Min(0) int xpReward,
            String metaLabel,
            String metaType,
            LocalDate dueDate
    ) {}

    public record UpdateRequest(
            UUID categoryId,
            @NotBlank @Size(max = 255) String title,
            String description,
            @Min(0) int xpReward,
            String status,
            String metaLabel,
            String metaType,
            LocalDate dueDate
    ) {}
}
