package com.succeshub.coreinfra.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public registration helpers used by the Keycloak enroll theme (live field checks).
 */
@Tag(name = "Registration", description = "Public username/email availability for self-registration")
@RestController
@RequestMapping("/public/registration")
@RequiredArgsConstructor
public class PublicRegistrationController {

    private final KeycloakAdminClient keycloakAdminClient;

    /**
     * Checks whether a username and/or email is already taken in Keycloak.
     * Used by the registration theme while the user types (debounced).
     */
    @Operation(
            summary = "Check registration availability",
            description = "Exact-match lookup against Keycloak users. Omit a param to skip that check."
    )
    @ApiResponse(responseCode = "200", description = "Availability returned")
    @GetMapping("/availability")
    public ResponseEntity<Map<String, Object>> availability(
            @Parameter(description = "Username to check (exact)")
            @RequestParam(required = false) String username,
            @Parameter(description = "Email to check (exact)")
            @RequestParam(required = false) String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (username != null && !username.isBlank()) {
            boolean taken = keycloakAdminClient.usernameExists(username);
            body.put("username", username.trim());
            body.put("usernameAvailable", !taken);
        }
        if (email != null && !email.isBlank()) {
            boolean taken = keycloakAdminClient.emailExists(email);
            body.put("email", email.trim());
            body.put("emailAvailable", !taken);
        }
        return ResponseEntity.ok(body);
    }
}
