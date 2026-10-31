package com.successhub.appdomain.dto.gamification;

import com.successhub.appdomain.dto.TaskDto;
import java.util.List;

public record DailyStatusDto(
        int missionsToday,
        int xpEarnedToday,
        int dailyXpRemaining,
        int currentStreak,
        String streakTier,
        int streakShields,
        /**
         * True when today qualifies and the user has not yet pressed Celebrate.
         * Distinct from streak settlement — {@code GET /daily} already lazy-closes past days.
         */
        boolean pendingCelebrations,
        /** True when today qualifies and Celebrate has already been acknowledged. */
        boolean daySealed,
        List<TaskDto.Response> weeklyChallenges,
        List<TaskDto.Response> scheduledToday
) {}
