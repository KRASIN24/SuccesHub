package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Atomically creates the initial profile row. Concurrent first requests for
     * the same user are collapsed by the database's unique key.
     */
    @Modifying
    @Query(value = """
            INSERT INTO succeshub.user_profile (id, keycloak_id, display_name, streak_tier)
            VALUES (:id, :keycloakId, :displayName, 'NONE')
            ON CONFLICT (keycloak_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("keycloakId") String keycloakId,
            @Param("displayName") String displayName);
}
