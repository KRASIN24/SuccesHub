package com.successhub.appdomain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class TaskCategoryDto {

    public record Response(
            UUID id,
            String name,
            String tag,
            boolean grantXp,
            int sortOrder
    ) {}

    public record CreateRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 50) String tag,
            boolean grantXp,
            int sortOrder
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 50) String tag,
            boolean grantXp,
            int sortOrder
    ) {}

    public record ReorderRequest(@NotEmpty List<UUID> orderedIds) {}
}
