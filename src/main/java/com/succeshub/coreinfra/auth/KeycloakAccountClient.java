package com.succeshub.coreinfra.auth;

import com.succeshub.config.KeycloakProperties;
import com.succeshub.coreinfra.exception_handler.base.AppException;
import com.succeshub.coreinfra.exception_handler.domain.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Calls Keycloak Account REST API with the logged-in user's access token.
 */
@Component
@RequiredArgsConstructor
public class KeycloakAccountClient {

    private final KeycloakProperties properties;
    private final RestClient.Builder restClientBuilder;

    /**
     * Changes the user's password via Account credentials API.
     *
     * @param authorizedClient session OAuth2 client holding the user access token
     * @param currentPassword  current password
     * @param newPassword      new password
     */
    public void changePassword(
            OAuth2AuthorizedClient authorizedClient,
            String currentPassword,
            String newPassword) {
        String token = requireAccessToken(authorizedClient);
        Map<String, String> body = Map.of(
                "currentPassword", currentPassword,
                "newPassword", newPassword,
                "confirmation", newPassword
        );
        try {
            restClientBuilder.build()
                    .post()
                    .uri(properties.accountUrl() + "/credentials/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 403) {
                throw new ValidationException("Current password is incorrect or the new password was rejected");
            }
            throw new AppException(
                    "Failed to change password in Keycloak",
                    HttpStatus.BAD_GATEWAY,
                    "KEYCLOAK_ACCOUNT_ERROR");
        }
    }

    private String requireAccessToken(OAuth2AuthorizedClient authorizedClient) {
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new AppException(
                    "No Keycloak access token in session — sign in again",
                    HttpStatus.UNAUTHORIZED,
                    "NO_ACCESS_TOKEN");
        }
        return authorizedClient.getAccessToken().getTokenValue();
    }
}
