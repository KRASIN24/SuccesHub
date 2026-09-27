package com.succeshub.appdomain.dto;

/**
 * Request body for updating UI theme and locale preferences on the profile.
 * Null fields are left unchanged.
 */
public record UpdatePreferencesRequest(
        Boolean darkTheme,
        String locale
) {}
