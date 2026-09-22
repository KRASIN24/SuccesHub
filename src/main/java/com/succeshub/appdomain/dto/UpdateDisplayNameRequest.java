package com.succeshub.appdomain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating the user's in-app display name from Settings.
 */
public record UpdateDisplayNameRequest(
        @NotBlank @Size(min = 1, max = 64) String displayName
) {}
