package com.successhub.appdomain.dto;

/**
 * Request body for updating UI and notification preferences on the profile.
 * Null fields are left unchanged.
 */
public record UpdatePreferencesRequest(
        Boolean darkTheme,
        String locale,
        Boolean notifyLootEarned,
        Boolean notifyAchievementUnlocked,
        Boolean notifyStreakMilestone,
        Boolean notifyCampaignDefeated,
        Boolean emailAchievementUnlocked,
        Boolean emailStreakMilestone,
        Boolean emailCampaignDefeated
) {}
