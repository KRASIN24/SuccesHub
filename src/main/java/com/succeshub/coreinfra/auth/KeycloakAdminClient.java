package com.succeshub.coreinfra.auth;

import com.succeshub.config.KeycloakProperties;
import com.succeshub.coreinfra.exception_handler.base.AppException;
import com.succeshub.coreinfra.exception_handler.domain.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Thin Keycloak Admin REST client using the {@code succeshub-admin} service account.
 */
@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private final KeycloakProperties properties;
    private final RestClient.Builder restClientBuilder;

    /**
     * Updates the user's email via Admin API.
     *
     * @param userId Keycloak user id ({@code sub})
     * @param email  new email address
     */
    public void updateEmail(String userId, String email) {
        Map<String, Object> user = getUser(userId);
        user.put("email", email);
        user.put("emailVerified", false);
        putUser(userId, user);
    }

    /**
     * Returns whether the user has an OTP credential configured.
     *
     * @param userId Keycloak user id
     * @return {@code true} when a {@code otp} credential exists
     */
    public boolean hasOtpCredential(String userId) {
        List<?> credentials = getCredentials(userId);
        return credentials.stream().anyMatch(c -> {
            if (c instanceof Map<?, ?> map) {
                Object type = map.get("type");
                return "otp".equals(type) || "totp".equals(type);
            }
            return false;
        });
    }

    /**
     * Deletes the Keycloak user.
     *
     * @param userId Keycloak user id
     */
    public void deleteUser(String userId) {
        try {
            adminClient().delete()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return;
            }
            throw mapAdminError("Failed to delete Keycloak user", ex);
        }
    }

    /**
     * Returns {@code true} when a user with this exact username already exists.
     *
     * @param username username to look up (exact match)
     * @return whether the username is taken
     */
    public boolean usernameExists(String username) {
        return !searchUsers("username", username).isEmpty();
    }

    /**
     * Returns {@code true} when a user with this exact email already exists.
     *
     * @param email email to look up (exact match)
     * @return whether the email is taken
     */
    public boolean emailExists(String email) {
        return !searchUsers("email", email).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchUsers(String param, String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            List<?> body = adminClient().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users")
                            .queryParam(param, value.trim())
                            .queryParam("exact", true)
                            .queryParam("max", 1)
                            .build())
                    .retrieve()
                    .body(List.class);
            if (body == null || body.isEmpty()) {
                return List.of();
            }
            return body.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        } catch (RestClientResponseException ex) {
            throw mapAdminError("Failed to search Keycloak users", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUser(String userId) {
        try {
            Map<String, Object> body = adminClient().get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .body(Map.class);
            if (body == null) {
                throw new AppException("Keycloak user not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
            }
            return body;
        } catch (RestClientResponseException ex) {
            throw mapAdminError("Failed to load Keycloak user", ex);
        }
    }

    private void putUser(String userId, Map<String, Object> user) {
        try {
            adminClient().put()
                    .uri("/users/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new ValidationException("Email is already in use");
            }
            throw mapAdminError("Failed to update Keycloak user", ex);
        }
    }

    private List<?> getCredentials(String userId) {
        try {
            List<?> body = adminClient().get()
                    .uri("/users/{id}/credentials", userId)
                    .retrieve()
                    .body(List.class);
            return body != null ? body : List.of();
        } catch (RestClientResponseException ex) {
            throw mapAdminError("Failed to load credentials", ex);
        }
    }

    private RestClient adminClient() {
        String token = fetchServiceAccountToken();
        return restClientBuilder.build()
                .mutate()
                .baseUrl(properties.adminBaseUrl())
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    private String fetchServiceAccountToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.adminClientId());
        form.add("client_secret", properties.adminClientSecret());

        try {
            Map<?, ?> body = restClientBuilder.build()
                    .post()
                    .uri(properties.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (body == null || body.get("access_token") == null) {
                throw new AppException(
                        "Keycloak admin token response missing access_token",
                        HttpStatus.BAD_GATEWAY,
                        "KEYCLOAK_ADMIN_TOKEN");
            }
            return body.get("access_token").toString();
        } catch (RestClientResponseException ex) {
            throw new AppException(
                    "Failed to obtain Keycloak admin token",
                    HttpStatus.BAD_GATEWAY,
                    "KEYCLOAK_ADMIN_TOKEN");
        }
    }

    private AppException mapAdminError(String message, RestClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        if (status.is4xxClientError()) {
            return new ValidationException(message + ": " + ex.getResponseBodyAsString());
        }
        return new AppException(message, HttpStatus.BAD_GATEWAY, "KEYCLOAK_ADMIN_ERROR");
    }
}
