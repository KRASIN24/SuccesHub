package com.succeshub.appdomain.dto;

public record ProfileDto(
        String keycloakId,
        String displayName,
        int level,
        int currentXp,
        int nextLevelXp,
        int currentStreak,
        int globalRank
) {}
