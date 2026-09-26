package com.succeshub.appdomain.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs for the in-app notification API.
 */
public final class NotificationDto {

    private NotificationDto() {}

    /**
     * A single notification row for the navbar dropdown.
     */
    public record Response(
            UUID id,
            String type,
            String title,
            String body,
            String actionUrl,
            boolean read,
            Instant createdAt
    ) {}

    /**
     * Unread badge payload.
     */
    public record UnreadCount(long count) {}
}
