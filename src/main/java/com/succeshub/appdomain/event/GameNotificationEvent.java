package com.succeshub.appdomain.event;

import com.succeshub.appdomain.model.NotificationType;

import java.util.UUID;

/**
 * Request to persist an in-app notification for the navbar bell.
 *
 * @param userId      Keycloak subject
 * @param type        notification category
 * @param title       short headline
 * @param body        supporting sentence
 * @param actionUrl   SPA deep-link (e.g. {@code /loot-boxes})
 * @param referenceId optional related entity id for debugging / future dedupe
 */
public record GameNotificationEvent(
        String userId,
        NotificationType type,
        String title,
        String body,
        String actionUrl,
        UUID referenceId
) {}
