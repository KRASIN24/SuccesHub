package com.succeshub.coreinfra.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Confirmation payload for account deletion.
 *
 * @param confirmation must equal {@code DELETE} (case-sensitive)
 */
public record DeleteAccountRequest(
        @NotBlank String confirmation
) {
}
