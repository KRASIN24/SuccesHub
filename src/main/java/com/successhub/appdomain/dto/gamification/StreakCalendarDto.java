package com.successhub.appdomain.dto.gamification;

import java.time.LocalDate;
import java.util.List;

/**
 * A month of streak activity plus live streak headline figures.
 *
 * @param month             first day of the rendered month
 * @param currentStreak     active consecutive-day streak
 * @param streakTier        current tier ({@code NONE}/{@code BRONZE}/{@code SILVER}/{@code GOLD})
 * @param streakShields     shields banked on the profile (auto-protection)
 * @param shieldsAvailable  total shields the user can spend (banked + inventory stacks)
 * @param minTasksPerDay    tasks required for a day to qualify
 * @param days              per-day statuses for the month
 */
public record StreakCalendarDto(
        LocalDate month,
        int currentStreak,
        String streakTier,
        int streakShields,
        int shieldsAvailable,
        int minTasksPerDay,
        List<StreakDayDto> days
) {}
