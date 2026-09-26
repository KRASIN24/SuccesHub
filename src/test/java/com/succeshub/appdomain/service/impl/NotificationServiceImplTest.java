package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.NotificationDto;
import com.succeshub.appdomain.event.GameNotificationEvent;
import com.succeshub.appdomain.model.Notification;
import com.succeshub.appdomain.model.NotificationType;
import com.succeshub.appdomain.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository);
    }

    @Test
    void onGameNotification_persistsRow() {
        // Arrange
        GameNotificationEvent event = new GameNotificationEvent(
                "user-1",
                NotificationType.LOOT_EARNED,
                "Loot earned",
                "A Glowing Orb awaits.",
                "/loot-boxes",
                UUID.randomUUID());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        // Act
        service.onGameNotification(event);

        // Assert
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals(NotificationType.LOOT_EARNED, saved.getType());
        assertEquals("/loot-boxes", saved.getActionUrl());
        assertTrue(!saved.isRead());
    }

    @Test
    void listRecent_capsLimitAndMapsDto() {
        // Arrange
        Notification row = new Notification();
        row.setId(UUID.randomUUID());
        row.setUserId("user-1");
        row.setType(NotificationType.ACHIEVEMENT_UNLOCKED);
        row.setTitle("Achievement unlocked");
        row.setBody("Pioneer");
        row.setActionUrl("/achievements");
        row.setRead(false);
        row.setCreatedAt(Instant.parse("2026-09-26T10:00:00Z"));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("user-1"), any(Pageable.class)))
                .thenReturn(List.of(row));

        // Act
        List<NotificationDto.Response> result = service.listRecent("user-1", 999);

        // Assert
        assertEquals(1, result.size());
        assertEquals("ACHIEVEMENT_UNLOCKED", result.getFirst().type());
        assertEquals("Pioneer", result.getFirst().body());
    }

    @Test
    void markRead_setsFlagWhenOwned() {
        // Arrange
        UUID id = UUID.randomUUID();
        Notification row = new Notification();
        row.setId(id);
        row.setUserId("user-1");
        row.setType(NotificationType.BOSS_DEFEATED);
        row.setTitle("Boss defeated");
        row.setBody("Hydra");
        row.setActionUrl("/goals");
        row.setRead(false);
        row.setCreatedAt(Instant.now());
        when(notificationRepository.findByIdAndUserId(id, "user-1")).thenReturn(Optional.of(row));
        when(notificationRepository.save(row)).thenReturn(row);

        // Act
        NotificationDto.Response dto = service.markRead("user-1", id);

        // Assert
        assertTrue(dto.read());
        verify(notificationRepository).save(row);
    }

    @Test
    void markRead_throwsWhenMissing() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(id, "user-1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> service.markRead("user-1", id));
    }

    @Test
    void markAllRead_returnsZeroUnread() {
        // Arrange
        when(notificationRepository.markAllRead("user-1")).thenReturn(3);

        // Act
        NotificationDto.UnreadCount count = service.markAllRead("user-1");

        // Assert
        assertEquals(0, count.count());
        verify(notificationRepository).markAllRead("user-1");
    }
}
