package com.succeshub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Keycloak connection settings used by account self-service (Account + Admin APIs).
 */
@ConfigurationProperties(prefix = "succeshub.keycloak")
public record KeycloakProperties(
        String serverUrl,
        String realm,
        String adminClientId,
        String adminClientSecret
) {
    public String realmBaseUrl() {
        return serverUrl.replaceAll("/$", "") + "/realms/" + realm;
    }

    public String adminBaseUrl() {
        return serverUrl.replaceAll("/$", "") + "/admin/realms/" + realm;
    }

    public String tokenUrl() {
        return realmBaseUrl() + "/protocol/openid-connect/token";
    }

    public String accountUrl() {
        return realmBaseUrl() + "/account";
    }

    public String authorizationUrl() {
        return realmBaseUrl() + "/protocol/openid-connect/auth";
    }
}
