package com.succeshub.appdomain.service.gamification;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Server-timezone day boundary helpers for V1 gamification.
 */
@Component
public class GamificationTimeUtil {

    private final ZoneId zone = ZoneId.systemDefault();

    public ZoneId zone() {
        return zone;
    }

    public LocalDate today() {
        return LocalDate.now(zone);
    }

    public Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(zone).toInstant();
    }

    public Instant endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(zone).toInstant();
    }

    public LocalDate mondayOfWeek(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1L);
    }
}
