package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.ProfileDto;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    /**
     * Returns the profile for the given Keycloak subject ID.
     * Creates a new profile row on first login.
     */
    @Transactional
    public ProfileDto getOrCreateProfile(String keycloakId, String displayName) {
        UserProfile profile = repository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setKeycloakId(keycloakId);
                    p.setDisplayName(displayName);
                    return repository.save(p);
                });

        if (displayName != null && !displayName.equals(profile.getDisplayName())) {
            profile.setDisplayName(displayName);
            profile = repository.save(profile);
        }

        return toDto(profile);
    }

    @Transactional
    public ProfileDto addXp(String keycloakId, int amount) {
        UserProfile profile = repository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalStateException("Profile not found for user: " + keycloakId));
        profile.addXp(amount);
        return toDto(repository.save(profile));
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
