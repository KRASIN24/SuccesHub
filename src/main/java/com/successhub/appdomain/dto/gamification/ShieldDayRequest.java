package com.successhub.appdomain.dto.gamification;

import java.time.LocalDate;

/** Request to protect a specific missed day by spending a streak shield. */
public record ShieldDayRequest(LocalDate date) {}
