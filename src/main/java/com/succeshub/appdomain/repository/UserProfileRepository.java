package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
