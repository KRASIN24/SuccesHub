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
            case LOOT_EARNED, ACHIEVEMENT_UNLOCKED, STREAK_MILESTONE, CAMPAIGN_DEFEATED
                    -> profile.isNotifyInApp();
            case TASK_REMINDER -> true;
        };
    }

    @Override
    public boolean isEmailEnabled(UserProfile profile, NotificationType type) {
        if (profile == null || type == null || !profile.isEmailNotifications()) {
            return false;
        }
        return switch (type) {
            case ACHIEVEMENT_UNLOCKED, STREAK_MILESTONE, CAMPAIGN_DEFEATED -> true;
            case LOOT_EARNED, TASK_REMINDER -> false;
        };
    }
}
