package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.ProfileDto;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.appdomain.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final Set<String> SUPPORTED_LOCALES = Set.of("en-US", "pl-PL");

    private final UserProfileRepository repository;

    @Override
    @Transactional
    public ProfileDto getOrCreateProfile(String keycloakId, String displayName) {
        UserProfile profile = repository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
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
    public ProfileDto updatePreferences(String keycloakId, Boolean darkTheme, String locale) {
        if (locale != null && !SUPPORTED_LOCALES.contains(locale)) {
            throw new IllegalArgumentException("Unsupported locale: " + locale);
        }

        UserProfile profile = repository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setKeycloakId(keycloakId);
                    return repository.save(p);
                });

        if (darkTheme != null) {
            profile.setDarkTheme(darkTheme);
        }
        if (locale != null) {
            profile.setLocale(locale);
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
                p.getLocale()
        );
    }
}
