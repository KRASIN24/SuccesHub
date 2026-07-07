package com.succeshub.appdomain.service;

import com.succeshub.appdomain.model.UserProfile;

import java.time.LocalDate;

/**
 * Streak tier and shield logic for resilient daily consistency tracking.
 */
public interface StreakService {

    /**
     * Applies the outcome of a processed calendar day to the user's streak.
     *
     * @param profile    user profile to mutate
     * @param day        calendar day being finalized
     * @param qualifying whether the user met {@code minTasksForQualifyingDay} on that day
     * @return {@code true} when a streak shield was consumed
     */
    boolean applyDayOutcome(UserProfile profile, LocalDate day, boolean qualifying);

    /**
     * Attempts to consume one streak shield from inventory, falling back to profile counter.
     *
     * @param userId  Keycloak subject ID
     * @param profile user profile
     * @return {@code true} when a shield was available and consumed
     */
    boolean consumeShield(String userId, UserProfile profile);
}
