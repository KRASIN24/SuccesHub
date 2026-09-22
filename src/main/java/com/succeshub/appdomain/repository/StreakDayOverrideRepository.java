package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.StreakDayOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for manual per-day streak overrides (e.g. shielded days).
 */
public interface StreakDayOverrideRepository extends JpaRepository<StreakDayOverride, UUID> {

    /**
     * Returns a user's day overrides within an inclusive date range.
     *
     * @param userId Keycloak subject ID of the owner
     * @param start  inclusive lower bound day
     * @param end    inclusive upper bound day
     * @return overrides falling within the range
     */
    List<StreakDayOverride> findByUserIdAndDayBetween(String userId, LocalDate start, LocalDate end);

    /**
     * Finds a single day override for a user.
     *
     * @param userId Keycloak subject ID of the owner
     * @param day    calendar day
     * @return the override when present
     */
    Optional<StreakDayOverride> findByUserIdAndDay(String userId, LocalDate day);

    /**
     * Reports whether the user already has an override on the given day.
     *
     * @param userId Keycloak subject ID of the owner
     * @param day    calendar day
     * @return {@code true} when an override exists
     */
    boolean existsByUserIdAndDay(String userId, LocalDate day);
}
