package com.successhub.coreinfra.mail;

import com.successhub.appdomain.enums.NotificationType;
import com.successhub.appdomain.event.GameNotificationEvent;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.coreinfra.auth.KeycloakAdminClient;
import com.successhub.coreinfra.security.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sends branded HTML mail for high-signal game milestones after the publishing
 * transaction commits. In-app bell notifications remain on {@code NotificationServiceImpl}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEmailListener {

    private static final Set<NotificationType> EMAIL_TYPES = EnumSet.of(
            NotificationType.ACHIEVEMENT_UNLOCKED,
            NotificationType.CAMPAIGN_DEFEATED,
            NotificationType.STREAK_MILESTONE);

    private final MailService mailService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final AuthProperties authProperties;
    private final UserProfileRepository userProfileRepository;

    /**
     * Emails achievement / boss / streak milestones when the user has an address in Keycloak.
     *
     * @param event game notification already used for the navbar bell
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGameNotification(GameNotificationEvent event) {
        if (event == null || event.type() == null || !EMAIL_TYPES.contains(event.type())) {
            return;
        }

        String email = keycloakAdminClient.findEmail(event.userId());
        if (email == null || email.isBlank()) {
            log.debug("Skipping game email {}: no Keycloak email for {}", event.type(), event.userId());
            return;
        }

        String template = switch (event.type()) {
            case ACHIEVEMENT_UNLOCKED -> "mail/achievement";
            case CAMPAIGN_DEFEATED -> "mail/campaign-defeated";
            case STREAK_MILESTONE -> "mail/streak-milestone";
            default -> null;
        };
        if (template == null) {
            return;
        }

        String subject = event.title() != null && !event.title().isBlank()
                ? event.title()
                : event.type().name();

        Map<String, Object> vars = new HashMap<>();
        vars.put("title", event.title());
        vars.put("body", event.body());
        vars.put("actionUrl", event.actionUrl());
        vars.put("frontendBaseUrl", authProperties.frontendOrigin());
        userProfileRepository.findByKeycloakId(event.userId())
                .ifPresent(profile -> {
                    if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
                        vars.put("displayName", profile.getDisplayName());
                    }
                });

        mailService.sendTemplateMail(email, subject, template, vars);
    }
}
