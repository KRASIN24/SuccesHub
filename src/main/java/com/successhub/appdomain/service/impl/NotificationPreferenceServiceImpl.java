package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.enums.NotificationType;
import com.successhub.appdomain.service.NotificationPreferenceService;
import org.springframework.stereotype.Service;

/**
 * Preference lookups for in-app bell rows and milestone emails.
 */
@Service
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    @Override
    public boolean isInAppEnabled(UserProfile profile, NotificationType type) {
        if (profile == null || type == null) {
            return true;
        }
        return switch (type) {
            case LOOT_EARNED -> profile.isNotifyLootEarned();
            case ACHIEVEMENT_UNLOCKED -> profile.isNotifyAchievementUnlocked();
            case STREAK_MILESTONE -> profile.isNotifyStreakMilestone();
            case CAMPAIGN_DEFEATED -> profile.isNotifyCampaignDefeated();
            case TASK_REMINDER -> true;
        };
    }

    @Override
    public boolean isEmailEnabled(UserProfile profile, NotificationType type) {
        if (profile == null || type == null) {
            return false;
        }
        return switch (type) {
            case ACHIEVEMENT_UNLOCKED -> profile.isEmailAchievementUnlocked();
            case STREAK_MILESTONE -> profile.isEmailStreakMilestone();
            case CAMPAIGN_DEFEATED -> profile.isEmailCampaignDefeated();
            case LOOT_EARNED, TASK_REMINDER -> false;
        };
    }
}
