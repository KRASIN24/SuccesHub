package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.NotificationDto;
import com.succeshub.appdomain.event.GameNotificationEvent;
import com.succeshub.appdomain.model.Notification;
import com.succeshub.appdomain.repository.NotificationRepository;
import com.succeshub.appdomain.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persists and serves in-app notifications for the navbar bell.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 20;

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto.Response> listRecent(String userId, int limit) {
        int capped = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, capped))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto.UnreadCount unreadCount(String userId) {
        return new NotificationDto.UnreadCount(notificationRepository.countByUserIdAndRead(userId, false));
    }

    @Override
    @Transactional
    public NotificationDto.Response markRead(String userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        return toDto(notification);
    }

    @Override
    @Transactional
    public NotificationDto.UnreadCount markAllRead(String userId) {
        notificationRepository.markAllRead(userId);
        return new NotificationDto.UnreadCount(0);
    }

    /**
     * Creates a notification row when a game event is published in-transaction.
     *
     * @param event publish request from loot / achievement / streak / boss paths
     */
    @EventListener
    @Transactional
    public void onGameNotification(GameNotificationEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.userId());
        notification.setType(event.type());
        notification.setTitle(event.title());
        notification.setBody(event.body());
        notification.setActionUrl(event.actionUrl());
        notification.setReferenceId(event.referenceId());
        notificationRepository.save(notification);
    }

    private NotificationDto.Response toDto(Notification n) {
        return new NotificationDto.Response(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getBody(),
                n.getActionUrl(),
                n.isRead(),
                n.getCreatedAt());
    }
}
