package com.successhub.appdomain.dto;

/**
 * Dashboard profile snapshot including UI theme, locale, and notification preferences.
 */
public record ProfileDto(
        String keycloakId,
        String displayName,
        int level,
        int currentXp,
        int nextLevelXp,
        int currentStreak,
        int globalRank,
        boolean darkTheme,
        String locale,
        boolean notifyInApp,
        boolean emailNotifications
) {}
