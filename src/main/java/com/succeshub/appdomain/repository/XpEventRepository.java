package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.XpEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistence access for XP audit events used in weekly insights.
 */
public interface XpEventRepository extends JpaRepository<XpEvent, UUID> {

    /**
     * Returns XP events for a user within a time range.
     *
     * @param userId    Keycloak subject ID
     * @param start     inclusive start instant
     * @param end       exclusive end instant
     * @return matching events ordered by creation time
     */
    List<XpEvent> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            String userId, Instant start, Instant end);

    /**
     * Counts task completions recorded as XP events for a user on a given day (server timezone bucketing via instant range).
     *
     * @param userId Keycloak subject ID
     * @param start  inclusive start of day
     * @param end    exclusive end of day
     * @return number of XP events in range
     */
    @Query("SELECT COUNT(e) FROM XpEvent e WHERE e.userId = :userId AND e.createdAt >= :start AND e.createdAt < :end")
    long countByUserIdAndDay(@Param("userId") String userId, @Param("start") Instant start, @Param("end") Instant end);

    /**
     * Returns whether XP was already granted for completing a specific task.
     *
     * @param userId Keycloak subject ID
     * @param taskId completed task ID
     * @return {@code true} when an XP event exists for the task
     */
    boolean existsByUserIdAndTaskId(String userId, UUID taskId);
}
