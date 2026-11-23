package com.successhub.appdomain.service;

import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.enums.NotificationType;

/**
 * Maps {@link NotificationType} to in-app and email preference flags on a profile.
 */
public interface NotificationPreferenceService {

    /**
     * Whether an in-app notification row should be created for {@code type}.
     * Unmapped types (e.g. task reminders) default to {@code true}.
     *
     * @param profile user profile with preference columns
     * @param type    game notification type
     * @return {@code true} when the bell row should be persisted
     */
    boolean isInAppEnabled(UserProfile profile, NotificationType type);

    /**
     * Whether a milestone email should be enqueued for {@code type}.
     * Types without an email flag (or {@code null} type) return {@code false}.
     *
     * @param profile user profile with preference columns
     * @param type    game notification type
     * @return {@code true} when mail may be enqueued
     */
    boolean isEmailEnabled(UserProfile profile, NotificationType type);
}
