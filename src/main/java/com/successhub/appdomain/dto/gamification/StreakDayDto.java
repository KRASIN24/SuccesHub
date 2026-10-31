package com.successhub.appdomain.dto.gamification;

import java.time.LocalDate;

/**
 * A single calendar day within a streak calendar month.
 *
 * @param date   the calendar day
 * @param status one of {@code COMPLETED}, {@code MISSED}, {@code SHIELDED}, {@code TODAY}, {@code FUTURE}, {@code EMPTY} (before profile creation)
 * @param completedTasks number of tasks completed on that day
 */
public record StreakDayDto(LocalDate date, String status, int completedTasks) {}
