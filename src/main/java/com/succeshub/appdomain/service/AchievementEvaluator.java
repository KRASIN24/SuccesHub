package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.AchievementDto;
import com.succeshub.appdomain.model.UserProfile;

import java.util.List;

/**
 * Evaluates and unlocks achievements based on profile and activity state.
 */
public interface AchievementEvaluator {

    /**
     * Checks all achievement rules and unlocks any newly earned merits.
     *
     * @param userId           Keycloak subject ID
     * @param profile          current profile snapshot
     * @param tasksCompletedToday tasks completed today (server timezone)
     * @return newly unlocked achievements
     */
    List<AchievementDto> evaluateAndUnlock(String userId, UserProfile profile, long tasksCompletedToday);
}
