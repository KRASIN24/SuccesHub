package com.succeshub.appdomain.dto;

/**
 * Dashboard profile snapshot including UI theme and locale preferences.
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
        String locale
) {}
