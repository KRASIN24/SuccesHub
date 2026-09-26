package com.succeshub.coreinfra.auth.dto;

import java.time.Instant;

/**
 * Account security status returned to Settings and the reactivate gate.
 *
 * @param email        current email from the OIDC principal / Keycloak
 * @param mfaEnabled   whether an OTP credential is configured
 * @param deactivated  whether the account is in the 30-day deletion grace period
 * @param purgeAt      when permanent delete is scheduled ({@code null} if active)
 */
public record AccountStatusResponse(
        String email,
        boolean mfaEnabled,
        boolean deactivated,
        Instant purgeAt
) {
}
