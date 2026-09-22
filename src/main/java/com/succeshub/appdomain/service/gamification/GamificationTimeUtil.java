package com.succeshub.appdomain.service.gamification;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-timezone day boundary helpers for V1 gamification.
 * Supports a process-wide day offset for local/dev testing (cleared on restart).
 */
@Component
public class GamificationTimeUtil {

    private final ZoneId zone = ZoneId.systemDefault();
    private final AtomicInteger dayOffset = new AtomicInteger(0);

    public ZoneId zone() {
        return zone;
    }

    /**
     * Effective "today" for gamification (real calendar date plus optional day offset).
     */
    public LocalDate today() {
        return realToday().plusDays(dayOffset.get());
    }

    /** Real calendar date with no testing offset applied. */
    public LocalDate realToday() {
        return LocalDate.now(zone);
    }

    public int getDayOffset() {
        return dayOffset.get();
    }

    /**
     * Sets the virtual day offset used by {@link #today()}.
     *
     * @param offset days to add to the real calendar (may be negative)
     */
    public void setDayOffset(int offset) {
        dayOffset.set(offset);
    }

    /** Clears any testing day offset so {@link #today()} matches the real calendar. */
    public void clearDayOffset() {
        dayOffset.set(0);
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
