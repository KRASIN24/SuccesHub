package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.gamification.GamificationDto.StreakActionResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.StreakCalendarDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.UseItemResultDto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * User-facing streak management: calendar rendering, shield/card usage, and
 * manual streak adjust/set/reset (gated at the controller by
 * {@code succeshub.loot.enable-dev-grants}).
 */
public interface StreakManagementService {

    /**
     * Builds a per-day activity calendar for a month plus live streak headline figures.
     *
     * @param userId Keycloak subject ID of the owner
     * @param month  month to render
     * @return calendar with each day classified as completed, missed, shielded, today, future, or empty (pre-account)
     */
    StreakCalendarDto getCalendar(String userId, YearMonth month);

    /**
     * Changes the current streak by a signed delta (dev/testing tool). Clamped at zero.
     *
     * @param userId Keycloak subject ID of the owner
     * @param delta  signed number of days to add or remove
     * @return updated streak snapshot
     */
    StreakActionResultDto adjustStreak(String userId, int delta);

    /**
     * Sets the current streak to an absolute non-negative value (dev/testing tool).
     *
     * @param userId Keycloak subject ID of the owner
     * @param value  new streak length; negative values are treated as zero
     * @return updated streak snapshot
     */
    StreakActionResultDto setStreak(String userId, int value);

    /**
     * Resets the current streak to zero and clears the tier (dev/testing tool).
     *
     * @param userId Keycloak subject ID of the owner
     * @return updated streak snapshot
     */
    StreakActionResultDto resetStreak(String userId);

    /**
     * Spends one streak shield to protect (repair) a specific missed day.
     * The day is recorded as shielded so lazy day-close never reverts it, and the streak grows by one.
     *
     * @param userId Keycloak subject ID of the owner
     * @param date   the missed day to protect; must not be in the future
     * @return updated streak snapshot
     * @throws com.succeshub.coreinfra.exception_handler.domain.ValidationException
     *         when the day is in the future, already qualifying/shielded, or no shield is available
     */
    StreakActionResultDto shieldDay(String userId, LocalDate date);

    /**
     * Consumes (uses) a utility inventory card, applying its effect.
     * Shields are banked for auto-protection; XP boosts grant XP immediately.
     *
     * @param userId      Keycloak subject ID of the owner
     * @param inventoryId inventory stack ID to consume from
     * @return result describing the applied effect and the updated profile
     * @throws jakarta.persistence.EntityNotFoundException when the item is missing or not owned
     * @throws com.succeshub.coreinfra.exception_handler.domain.ValidationException
     *         when the item is out of stock or not a consumable card
     */
    UseItemResultDto useInventoryItem(String userId, UUID inventoryId);
}
