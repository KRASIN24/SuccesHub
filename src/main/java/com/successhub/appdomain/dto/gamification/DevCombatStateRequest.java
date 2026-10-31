package com.successhub.appdomain.dto.gamification;

/**
 * Request body for setting combat-related profile flags.
 * Null fields leave the existing value unchanged.
 */
public record DevCombatStateRequest(Integer streakShields, Boolean pendingDoubleStrike, Boolean pendingSurgeToken) {}
