package com.succeshub.coreinfra.auth;

import com.succeshub.coreinfra.auth.dto.AccountStatusResponse;
import com.succeshub.coreinfra.auth.dto.MfaSetupResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * In-app account self-service backed by Keycloak Account + Admin APIs,
 * including 30-day cancelable deactivation.
 */
public interface AccountService {

    /**
     * Returns email, MFA, and deactivation status for Settings / reactivate gate.
     *
     * @param principal authenticated OIDC user
     * @return status DTO
     */
    AccountStatusResponse getStatus(OidcUser principal);

    /**
     * Updates the user's email in Keycloak.
     *
     * @param principal authenticated user
     * @param email     new email
     */
    void updateEmail(OidcUser principal, String email);

    /**
     * Changes password via Keycloak Account API using the session access token.
     *
     * @param principal        authenticated user
     * @param authorizedClient OAuth2 client from the BFF session
     * @param currentPassword  current password
     * @param newPassword      new password
     */
    void changePassword(
            OidcUser principal,
            OAuth2AuthorizedClient authorizedClient,
            String currentPassword,
            String newPassword);

    /**
     * Returns a same-origin URL that starts Keycloak AIA for TOTP setup.
     *
     * @return redirect target for the browser
     */
    MfaSetupResponse mfaSetupRedirect();

    /**
     * Starts the 30-day deletion grace period (does not purge data yet).
     * Caller should end the HTTP session afterwards.
     *
     * @param principal    authenticated user
     * @param confirmation must be {@code DELETE}
     */
    void deleteAccount(OidcUser principal, String confirmation);

    /**
     * Cancels a pending deletion by clearing {@code deactivated_at}.
     *
     * @param principal authenticated user
     */
    void reactivate(OidcUser principal);
}
