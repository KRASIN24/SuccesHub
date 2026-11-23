package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.enums.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPreferenceServiceImplTest {

    private final NotificationPreferenceServiceImpl service = new NotificationPreferenceServiceImpl();

    @Test
    void inAppMasterSwitchGatesGameTypesAndKeepsTaskReminderOn() {
        UserProfile profile = new UserProfile();
        profile.setNotifyInApp(false);

        assertFalse(service.isInAppEnabled(profile, NotificationType.LOOT_EARNED));
        assertFalse(service.isInAppEnabled(profile, NotificationType.ACHIEVEMENT_UNLOCKED));
        assertFalse(service.isInAppEnabled(profile, NotificationType.CAMPAIGN_DEFEATED));
        assertTrue(service.isInAppEnabled(profile, NotificationType.TASK_REMINDER));

        profile.setNotifyInApp(true);
        assertTrue(service.isInAppEnabled(profile, NotificationType.STREAK_MILESTONE));
    }

    @Test
    void emailMasterSwitchOnlyForMilestoneTypes() {
        UserProfile profile = new UserProfile();
        profile.setEmailNotifications(true);

        assertTrue(service.isEmailEnabled(profile, NotificationType.ACHIEVEMENT_UNLOCKED));
        assertTrue(service.isEmailEnabled(profile, NotificationType.STREAK_MILESTONE));
        assertTrue(service.isEmailEnabled(profile, NotificationType.CAMPAIGN_DEFEATED));
        assertFalse(service.isEmailEnabled(profile, NotificationType.LOOT_EARNED));

        profile.setEmailNotifications(false);
        assertFalse(service.isEmailEnabled(profile, NotificationType.ACHIEVEMENT_UNLOCKED));
    }
}
