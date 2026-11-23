package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.dto.ProfileDto;
import com.successhub.appdomain.dto.UpdatePreferencesRequest;
import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.appdomain.service.UserProfileService;
import com.successhub.coreinfra.mail.MailLocales;
import com.successhub.coreinfra.mail.MailService;
import com.successhub.coreinfra.security.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final Set<String> SUPPORTED_LOCALES = Set.of("en-US", "pl-PL");

    private final UserProfileRepository repository;
    private final MailService mailService;
    private final AuthProperties authProperties;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public ProfileDto getOrCreateProfile(String keycloakId, String displayName, String email) {
        var existing = repository.findByKeycloakId(keycloakId);
        boolean created = existing.isEmpty();

        UserProfile profile = existing.orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setKeycloakId(keycloakId);
            p.setDisplayName(displayName);
            return repository.save(p);
        });

        if (displayName != null
                && !displayName.equals(profile.getDisplayName())
                && !profile.isDisplayNameCustomized()) {
            profile.setDisplayName(displayName);
            profile = repository.save(profile);
        }

        if (created) {
            Locale locale = MailLocales.fromProfile(profile.getLocale());
            String name = displayName != null && !displayName.isBlank()
                    && !"commander".equalsIgnoreCase(displayName.trim())
                    ? displayName.trim()
                    : null;
            Map<String, Object> vars = new HashMap<>();
            vars.put("kicker", messageSource.getMessage("mail.welcome.kicker", null, locale));
            vars.put("heading", name != null
                    ? messageSource.getMessage("mail.welcome.titleNamed", new Object[]{name}, locale)
                    : messageSource.getMessage("mail.welcome.titleAnon", null, locale));
            vars.put("lead", messageSource.getMessage("mail.welcome.lead", null, locale));
            vars.put("ctaLabel", messageSource.getMessage("mail.welcome.cta", null, locale));
            vars.put("footer", messageSource.getMessage("mail.footer", null, locale));
            vars.put("frontendBaseUrl", authProperties.frontendOrigin());
            mailService.sendTemplateMail(
                    email,
                    messageSource.getMessage("mail.welcome.subject", null, locale),
                    "mail/welcome",
                    vars,
                    keycloakId,
                    locale);
        }

        return toDto(profile);
    }

    @Override
    @Transactional
    public ProfileDto updateDisplayName(String keycloakId, String displayName) {
        UserProfile profile = repository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setKeycloakId(keycloakId);
                    return repository.save(p);
                });

        profile.setDisplayName(displayName.trim());
        profile.setDisplayNameCustomized(true);
        return toDto(repository.save(profile));
    }

    @Override
    @Transactional
    public ProfileDto updatePreferences(String keycloakId, UpdatePreferencesRequest request) {
        if (request == null) {
            request = new UpdatePreferencesRequest(null, null, null, null);
        }
        String locale = request.locale();
        if (locale != null && !SUPPORTED_LOCALES.contains(locale)) {
            throw new IllegalArgumentException("Unsupported locale: " + locale);
        }

        UserProfile profile = repository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setKeycloakId(keycloakId);
                    return repository.save(p);
                });

        if (request.darkTheme() != null) {
            profile.setDarkTheme(request.darkTheme());
        }
        if (locale != null) {
            profile.setLocale(locale);
        }
        if (request.notifyInApp() != null) {
            profile.setNotifyInApp(request.notifyInApp());
        }
        if (request.emailNotifications() != null) {
            profile.setEmailNotifications(request.emailNotifications());
        }
        return toDto(repository.save(profile));
    }

    @Override
    @Transactional
    public ProfileDto addXp(String keycloakId, int amount) {
        UserProfile profile = repository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalStateException("Profile not found for user: " + keycloakId));
        profile.addXp(amount);
        return toDto(repository.save(profile));
    }

    @Override
    @Transactional
    public UserProfile requireProfile(String keycloakId) {
        return repository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile();
                    profile.setKeycloakId(keycloakId);
                    return repository.save(profile);
                });
    }

    @Override
    public void resetDailyCountersIfNeeded(UserProfile profile, java.time.LocalDate today) {
        if (profile.getDailyXpDate() == null || !profile.getDailyXpDate().equals(today)) {
            profile.setDailyXpDate(today);
            profile.setDailyXpEarned(0);
            profile.setFirstTaskCompletedToday(false);
        }
    }

    @Override
    public ProfileDto mapToDto(UserProfile profile) {
        return toDto(profile);
    }

    private ProfileDto toDto(UserProfile p) {
        return new ProfileDto(
                p.getKeycloakId(),
                p.getDisplayName(),
                p.getLevel(),
                p.getCurrentXp(),
                p.getNextLevelXp(),
                p.getCurrentStreak(),
                p.getGlobalRank(),
                p.isDarkTheme(),
                p.getLocale(),
                p.isNotifyInApp(),
                p.isEmailNotifications()
        );
    }
}
