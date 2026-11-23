package com.successhub.coreinfra.mail;

import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.enums.NotificationType;
import com.successhub.appdomain.event.GameNotificationEvent;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.appdomain.service.NotificationPreferenceService;
import com.successhub.coreinfra.auth.KeycloakAdminClient;
import com.successhub.coreinfra.security.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern STREAK_BODY = Pattern.compile("^You earned a (.*) for your streak\\.$");
    private static final Pattern CAMPAIGN_BODY = Pattern.compile("^(.*) has been slain\\.$");

    private final MailService mailService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final AuthProperties authProperties;
    private final UserProfileRepository userProfileRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final MessageSource messageSource;

    /**
     * Emails achievement / campaign / streak milestones when the user has an address in Keycloak
     * and the matching email preference is enabled. Copy follows the profile locale.
     *
     * @param event game notification already used for the navbar bell
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGameNotification(GameNotificationEvent event) {
        if (event == null || event.type() == null || !EMAIL_TYPES.contains(event.type())) {
            return;
        }

        UserProfile profile = userProfileRepository.findByKeycloakId(event.userId()).orElse(null);
        if (profile != null && !notificationPreferenceService.isEmailEnabled(profile, event.type())) {
            log.debug("Skipping game email {}: email preference off for {}", event.type(), event.userId());
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

        Locale locale = MailLocales.fromProfile(profile != null ? profile.getLocale() : null);
        String prefix = switch (event.type()) {
            case ACHIEVEMENT_UNLOCKED -> "mail.achievement";
            case CAMPAIGN_DEFEATED -> "mail.campaign";
            case STREAK_MILESTONE -> "mail.streak";
            default -> null;
        };
        if (prefix == null) {
            return;
        }

        String displayName = null;
        if (profile != null
                && profile.getDisplayName() != null
                && !profile.getDisplayName().isBlank()
                && !"commander".equalsIgnoreCase(profile.getDisplayName().trim())) {
            displayName = profile.getDisplayName().trim();
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("kicker", msg(prefix + ".kicker", locale));
        vars.put("heading", msg(prefix + ".subject", locale));
        vars.put("greeting", displayName != null
                ? msg(prefix + ".greetingNamed", locale, displayName)
                : msg(prefix + ".greetingAnon", locale));
        vars.put("detail", detailFor(event, locale));
        vars.put("ctaLabel", msg(prefix + ".cta", locale));
        vars.put("footer", msg("mail.footer", locale));
        vars.put("actionUrl", event.actionUrl());
        vars.put("frontendBaseUrl", authProperties.frontendOrigin());
        vars.put("displayName", displayName);

        String subject = msg(prefix + ".subject", locale);
        mailService.sendTemplateMail(email, subject, template, vars, event.userId(), locale);
    }

    private String detailFor(GameNotificationEvent event, Locale locale) {
        String body = event.body() != null ? event.body() : "";
        return switch (event.type()) {
            case STREAK_MILESTONE -> {
                Matcher m = STREAK_BODY.matcher(body);
                String box = m.matches() ? m.group(1) : body;
                yield msg("mail.streak.detail", locale, box);
            }
            case CAMPAIGN_DEFEATED -> {
                Matcher m = CAMPAIGN_BODY.matcher(body);
                String name = m.matches() ? m.group(1) : body;
                yield msg("mail.campaign.detail", locale, name);
            }
            case ACHIEVEMENT_UNLOCKED -> msg("mail.achievement.detail", locale, body);
            default -> body;
        };
    }

    private String msg(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }
}
