package com.succeshub.coreinfra.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating the Keycloak account email.
 */
public record UpdateEmailRequest(
        @NotBlank @Email @Size(max = 254) String email
) {
}
