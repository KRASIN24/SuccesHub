package com.successhub.appdomain.dto.gamification;

/**
 * Request body for mutating campaign HP / pending Strike bank.
 * Null fields leave the existing value unchanged.
 */
public record DevCampaignStateRequest(Double currentProgress, Double pendingDamage) {}
