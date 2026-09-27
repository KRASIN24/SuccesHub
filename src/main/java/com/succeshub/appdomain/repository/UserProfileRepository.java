package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for {@link UserProfile} rows keyed by Keycloak subject ID.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Looks up the profile belonging to the given Keycloak subject.
     *
     * @param keycloakId OIDC subject ({@code sub}) from the authenticated session
     * @return the matching profile, or empty when the user has no row yet
     */
    Optional<UserProfile> findByKeycloakId(String keycloakId);

    /**
     * Deletes the profile row for a Keycloak subject (account deletion).
     *
     * @param keycloakId OIDC subject ({@code sub})
     */
    void deleteByKeycloakId(String keycloakId);

    /**
     * Profiles whose deactivation grace period has ended (ready for permanent purge).
     *
     * @param cutoff instant before which {@code deactivated_at} means purge is due
     * @return profiles to hard-delete
     */
    List<UserProfile> findByDeactivatedAtBefore(Instant cutoff);
}
