package com.succeshub.appdomain.service;

/**
 * Cascades deletion of all SuccessHub rows owned by a Keycloak subject.
 */
public interface UserDataCleanupService {

    /**
     * Removes local app data for the given Keycloak user id.
     *
     * @param keycloakUserId OIDC {@code sub}
     */
    void deleteAllForUser(String keycloakUserId);
}
