package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.dto.NotificationDto;
import com.successhub.appdomain.entity.Notification;
import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.enums.NotificationType;
import com.successhub.appdomain.event.GameNotificationEvent;
import com.successhub.appdomain.repository.NotificationRepository;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.appdomain.service.NotificationPreferenceService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock NotificationPreferenceService notificationPreferenceService;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(
                notificationRepository, userProfileRepository, notificationPreferenceService);
    }

    @Test
    void onGameNotification_persistsRowWhenEnabled() {
        GameNotificationEvent event = new GameNotificationEvent(
                "user-1",
                NotificationType.LOOT_EARNED,
                "Loot earned",
                "A Arcane Orb awaits.",
                "/loot-boxes",
                UUID.randomUUID());
        UserProfile profile = new UserProfile();
        when(userProfileRepository.findByKeycloakId("user-1")).thenReturn(Optional.of(profile));
        when(notificationPreferenceService.isInAppEnabled(profile, NotificationType.LOOT_EARNED)).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        service.onGameNotification(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals(NotificationType.LOOT_EARNED, saved.getType());
        assertEquals("/loot-boxes", saved.getActionUrl());
        assertTrue(!saved.isRead());
    }

    @Test
    void onGameNotification_skipsWhenNotifyPreferenceOff() {
        GameNotificationEvent event = new GameNotificationEvent(
                "user-1",
                NotificationType.ACHIEVEMENT_UNLOCKED,
                "Achievement unlocked",
                "Pioneer",
                "/achievements",
                null);
        UserProfile profile = new UserProfile();
        when(userProfileRepository.findByKeycloakId("user-1")).thenReturn(Optional.of(profile));
        when(notificationPreferenceService.isInAppEnabled(profile, NotificationType.ACHIEVEMENT_UNLOCKED))
                .thenReturn(false);

        service.onGameNotification(event);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void listRecent_capsLimitAndMapsDto() {
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

        List<NotificationDto.Response> result = service.listRecent("user-1", 999);

        assertEquals(1, result.size());
        assertEquals("ACHIEVEMENT_UNLOCKED", result.getFirst().type());
        assertEquals("Pioneer", result.getFirst().body());
    }

    @Test
    void markRead_setsFlagWhenOwned() {
        UUID id = UUID.randomUUID();
        Notification row = new Notification();
        row.setId(id);
        row.setUserId("user-1");
        row.setType(NotificationType.CAMPAIGN_DEFEATED);
        row.setTitle("Campaign defeated");
        row.setBody("Hydra");
        row.setActionUrl("/goals");
        row.setRead(false);
        row.setCreatedAt(Instant.now());
        when(notificationRepository.findByIdAndUserId(id, "user-1")).thenReturn(Optional.of(row));
        when(notificationRepository.save(row)).thenReturn(row);

        NotificationDto.Response dto = service.markRead("user-1", id);

        assertTrue(dto.read());
        verify(notificationRepository).save(row);
    }

    @Test
    void markRead_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(id, "user-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.markRead("user-1", id));
    }
}
