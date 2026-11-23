package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.enums.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPreferenceServiceImplTest {

    private final NotificationPreferenceServiceImpl service = new NotificationPreferenceServiceImpl();

    @Test
    void inAppRespectsFlagsAndDefaultsTaskReminderOn() {
        UserProfile profile = new UserProfile();
        profile.setNotifyLootEarned(false);
        profile.setNotifyAchievementUnlocked(true);

        assertFalse(service.isInAppEnabled(profile, NotificationType.LOOT_EARNED));
        assertTrue(service.isInAppEnabled(profile, NotificationType.ACHIEVEMENT_UNLOCKED));
        assertTrue(service.isInAppEnabled(profile, NotificationType.TASK_REMINDER));
    }

    @Test
    void emailOnlyForMilestoneTypes() {
        UserProfile profile = new UserProfile();
        profile.setEmailAchievementUnlocked(true);
        profile.setEmailStreakMilestone(false);
        profile.setEmailCampaignDefeated(true);

        assertTrue(service.isEmailEnabled(profile, NotificationType.ACHIEVEMENT_UNLOCKED));
        assertFalse(service.isEmailEnabled(profile, NotificationType.STREAK_MILESTONE));
        assertTrue(service.isEmailEnabled(profile, NotificationType.CAMPAIGN_DEFEATED));
        assertFalse(service.isEmailEnabled(profile, NotificationType.LOOT_EARNED));
    }
}
