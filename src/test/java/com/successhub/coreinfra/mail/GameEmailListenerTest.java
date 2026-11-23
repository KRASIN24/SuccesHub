package com.successhub.coreinfra.mail;

import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.enums.NotificationType;
import com.successhub.appdomain.event.GameNotificationEvent;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.coreinfra.auth.KeycloakAdminClient;
import com.successhub.coreinfra.security.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameEmailListenerTest {

    @Mock MailService mailService;
    @Mock KeycloakAdminClient keycloakAdminClient;
    @Mock UserProfileRepository userProfileRepository;

    AuthProperties authProperties;
    GameEmailListener listener;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.setFrontendUrl("http://localhost:4200/");
        listener = new GameEmailListener(mailService, keycloakAdminClient, authProperties, userProfileRepository);
    }

    @Test
    void sendsAchievementMail() {
        when(keycloakAdminClient.findEmail("kc-1")).thenReturn("a@b.c");
        UserProfile profile = new UserProfile();
        profile.setDisplayName("Ada");
        when(userProfileRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));

        listener.onGameNotification(new GameNotificationEvent(
                "kc-1",
                NotificationType.ACHIEVEMENT_UNLOCKED,
                "Achievement unlocked",
                "Pioneer",
                "/achievements",
                UUID.randomUUID()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendTemplateMail(
                eq("a@b.c"),
                eq("Achievement unlocked"),
                eq("mail/achievement"),
                vars.capture());
        assertEquals("Ada", vars.getValue().get("displayName"));
        assertEquals("http://localhost:4200", vars.getValue().get("frontendBaseUrl"));
        assertEquals("/achievements", vars.getValue().get("actionUrl"));
    }

    @Test
    void skipsLootEarned() {
        listener.onGameNotification(new GameNotificationEvent(
                "kc-1",
                NotificationType.LOOT_EARNED,
                "Loot",
                "box",
                "/loot-boxes",
                null));

        verify(mailService, never()).sendTemplateMail(anyString(), anyString(), anyString(), anyMap());
        verify(keycloakAdminClient, never()).findEmail(any());
    }

    @Test
    void skipsBlankEmail() {
        when(keycloakAdminClient.findEmail("kc-1")).thenReturn(null);

        listener.onGameNotification(new GameNotificationEvent(
                "kc-1",
                NotificationType.CAMPAIGN_DEFEATED,
                "Campaign defeated",
                "fallen",
                "/goals",
                null));

        verify(mailService, never()).sendTemplateMail(anyString(), anyString(), anyString(), anyMap());
    }
}
