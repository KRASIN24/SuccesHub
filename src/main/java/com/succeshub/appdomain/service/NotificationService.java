package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.NotificationDto;

import java.util.List;
import java.util.UUID;

/**
 * In-app notification inbox for the navbar bell (list, unread, mark read).
 */
public interface NotificationService {

    /**
     * Returns the newest notifications for the user.
     *
     * @param userId Keycloak subject
     * @param limit  max rows (capped server-side)
     * @return newest-first list
     */
    List<NotificationDto.Response> listRecent(String userId, int limit);

    /**
     * Returns how many unread notifications the user has.
     *
     * @param userId Keycloak subject
     * @return unread count DTO
     */
    NotificationDto.UnreadCount unreadCount(String userId);

    /**
     * Marks one notification as read when owned by the user.
     *
     * @param userId         Keycloak subject
     * @param notificationId row id
     * @return updated row
     * @throws jakarta.persistence.EntityNotFoundException when missing or not owned
     */
    NotificationDto.Response markRead(String userId, UUID notificationId);

    /**
     * Marks all unread notifications for the user as read.
     *
     * @param userId Keycloak subject
     * @return unread count after the update (always 0)
     */
    NotificationDto.UnreadCount markAllRead(String userId);
}
