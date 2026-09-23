package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.ProfileDto;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.appdomain.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository repository;

    @Override
    @Transactional
    public ProfileDto getOrCreateProfile(String keycloakId, String displayName) {
        UserProfile profile = findOrCreateProfile(keycloakId, displayName);

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
        UserProfile profile = findOrCreateProfile(keycloakId, null);

        profile.setDisplayName(displayName.trim());
        profile.setDisplayNameCustomized(true);
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
        return findOrCreateProfile(keycloakId, null);
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

    private UserProfile findOrCreateProfile(String keycloakId, String displayName) {
        return repository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    repository.insertIfAbsent(UUID.randomUUID(), keycloakId, displayName);
                    return repository.findByKeycloakId(keycloakId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Profile creation did not produce a row for user: " + keycloakId));
                });
    }

    private ProfileDto toDto(UserProfile p) {
        return new ProfileDto(
                p.getKeycloakId(),
                p.getDisplayName(),
                p.getLevel(),
                p.getCurrentXp(),
                p.getNextLevelXp(),
                p.getCurrentStreak(),
                p.getGlobalRank()
        );
    }
}
