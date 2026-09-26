package com.succeshub.coreinfra.auth.dto;

/**
 * Points the SPA at the BFF OAuth2 entry that starts a Keycloak AIA.
 *
 * @param redirectUrl relative path such as {@code /oauth2/authorization/keycloak?kc_action=CONFIGURE_TOTP}
 */
public record MfaSetupResponse(String redirectUrl) {
}
