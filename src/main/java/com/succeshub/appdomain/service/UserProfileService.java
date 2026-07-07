package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.ProfileDto;
import com.succeshub.appdomain.model.UserProfile;

/**
 * Application service for user profiles, XP totals, and level progression.
 */
public interface UserProfileService {

    /**
     * Returns the profile for the given Keycloak subject ID.
     * Creates a new profile row on first access.
     *
     * @param keycloakId  OIDC subject ({@code sub}) from the authenticated session
     * @param displayName human-readable name synced from Keycloak; may be {@code null}
     * @return current profile state exposed to the dashboard
     */
    ProfileDto getOrCreateProfile(String keycloakId, String displayName);

    /**
     * Adds XP to the user's profile and applies any resulting level-ups.
     *
     * @param keycloakId OIDC subject ({@code sub}) of the user receiving XP
     * @param amount     XP points to add; must be positive in normal flows
     * @return updated profile after persistence
     * @throws IllegalStateException when no profile exists for the given user
     */
    ProfileDto addXp(String keycloakId, int amount);

    /**
     * Loads the profile entity for gamification flows, creating a row on first access.
     *
     * @param keycloakId OIDC subject ({@code sub}) of the user
     * @return persisted profile entity
     */
    UserProfile requireProfile(String keycloakId);

    /**
     * Resets daily XP counters when the calendar day changes (server timezone in V1).
     *
     * @param profile profile entity to update in memory
     * @param today   current local date used for day boundaries
     */
    void resetDailyCountersIfNeeded(UserProfile profile, java.time.LocalDate today);

    /**
     * Maps a persisted profile entity to the API DTO.
     *
     * @param profile profile entity
     * @return dashboard profile snapshot
     */
    ProfileDto mapToDto(UserProfile profile);
}
