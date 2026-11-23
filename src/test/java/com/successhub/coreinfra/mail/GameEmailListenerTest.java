package com.successhub.coreinfra.mail;

import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.enums.NotificationType;
import com.successhub.appdomain.event.GameNotificationEvent;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.appdomain.service.NotificationPreferenceService;
import com.successhub.coreinfra.auth.KeycloakAdminClient;
import com.successhub.coreinfra.security.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameEmailListenerTest {

    @Mock MailService mailService;
    @Mock KeycloakAdminClient keycloakAdminClient;
    @Mock UserProfileRepository userProfileRepository;
    @Mock NotificationPreferenceService notificationPreferenceService;
    @Mock MessageSource messageSource;

    AuthProperties authProperties;
    GameEmailListener listener;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.setFrontendUrl("http://localhost:4200/");
        listener = new GameEmailListener(
                mailService,
                keycloakAdminClient,
                authProperties,
                userProfileRepository,
                notificationPreferenceService,
                messageSource);
        when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
                .thenAnswer(inv -> {
                    String code = inv.getArgument(0);
                    Object[] args = inv.getArgument(1);
                    if (args != null && args.length > 0) {
                        return code + ":" + args[0];
                    }
                    return code;
                });
    }

    @Test
    void afterCommitListenerStartsNewTransactionForOutboxWrite() throws NoSuchMethodException {
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(
                GameEmailListener.class.getMethod("onGameNotification", GameNotificationEvent.class),
                Transactional.class);

        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    @Test
    void sendsAchievementMailInProfileLocale() {
        UserProfile profile = new UserProfile();
        profile.setDisplayName("Ada");
        profile.setLocale("pl-PL");
        when(userProfileRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(notificationPreferenceService.isEmailEnabled(profile, NotificationType.ACHIEVEMENT_UNLOCKED))
                .thenReturn(true);
        when(keycloakAdminClient.findEmail("kc-1")).thenReturn("a@b.c");

        listener.onGameNotification(new GameNotificationEvent(
                "kc-1",
                NotificationType.ACHIEVEMENT_UNLOCKED,
                "Achievement unlocked",
                "Pioneer",
                "/achievements",
                UUID.randomUUID()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Locale> locale = ArgumentCaptor.forClass(Locale.class);
        verify(mailService).sendTemplateMail(
                eq("a@b.c"),
                eq("mail.achievement.subject"),
                eq("mail/achievement"),
                vars.capture(),
                eq("kc-1"),
                locale.capture());
        assertEquals("pl-PL", locale.getValue().toLanguageTag());
        assertEquals("Ada", vars.getValue().get("displayName"));
        assertEquals("mail.achievement.greetingNamed:Ada", vars.getValue().get("greeting"));
        assertEquals("/achievements", vars.getValue().get("actionUrl"));
    }

    @Test
    void skipsCommanderAsDisplayName() {
        UserProfile profile = new UserProfile();
        profile.setDisplayName("commander");
        profile.setLocale("en-US");
        when(userProfileRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(notificationPreferenceService.isEmailEnabled(profile, NotificationType.STREAK_MILESTONE))
                .thenReturn(true);
        when(keycloakAdminClient.findEmail("kc-1")).thenReturn("a@b.c");

        listener.onGameNotification(new GameNotificationEvent(
                "kc-1",
                NotificationType.STREAK_MILESTONE,
                "Streak milestone",
                "You earned a Iron Chest for your streak.",
                "/vault",
                null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendTemplateMail(
                eq("a@b.c"),
                anyString(),
                eq("mail/streak-milestone"),
                vars.capture(),
                eq("kc-1"),
                any(Locale.class));
        assertEquals("mail.streak.greetingAnon", vars.getValue().get("greeting"));
        assertEquals(null, vars.getValue().get("displayName"));
    }

    @Test
    void skipsWhenEmailPreferenceOff() {
        UserProfile profile = new UserProfile();
        when(userProfileRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(notificationPreferenceService.isEmailEnabled(profile, NotificationType.ACHIEVEMENT_UNLOCKED))
                .thenReturn(false);

        listener.onGameNotification(new GameNotificationEvent(
                "kc-1",
                NotificationType.ACHIEVEMENT_UNLOCKED,
                "Achievement unlocked",
                "Pioneer",
                "/achievements",
                null));

        verify(keycloakAdminClient, never()).findEmail(any());
        verify(mailService, never()).sendTemplateMail(
                anyString(), anyString(), anyString(), anyMap(), any(), any());
    }

    @Test
    void skipsLootEarned() {
        listener.onGameNotification(new GameNotificationEvent(
                "kc-1",
                NotificationType.LOOT_EARNED,
                "Loot",
                "box",
                "/vault",
                null));

        verify(mailService, never()).sendTemplateMail(
                anyString(), anyString(), anyString(), anyMap(), any(), any());
        verify(keycloakAdminClient, never()).findEmail(any());
    }

    @Test
    void skipsBlankEmail() {
        UserProfile profile = new UserProfile();
        when(userProfileRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(notificationPreferenceService.isEmailEnabled(profile, NotificationType.CAMPAIGN_DEFEATED))
                .thenReturn(true);
        when(keycloakAdminClient.findEmail("kc-1")).thenReturn(null);

        listener.onGameNotification(new GameNotificationEvent(
                "kc-1",
                NotificationType.CAMPAIGN_DEFEATED,
                "Campaign defeated",
                "fallen",
                "/campaigns",
                null));

        verify(mailService, never()).sendTemplateMail(
                anyString(), anyString(), anyString(), anyMap(), any(), any());
    }
}
