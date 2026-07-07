package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for user-owned {@link Task} entities.
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /**
     * Returns all tasks for a user, newest first.
     *
     * @param userId Keycloak subject ID of the task owner
     * @return tasks ordered by {@code createdAt} descending
     */
    List<Task> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Returns tasks for a user filtered by workflow status.
     *
     * @param userId Keycloak subject ID of the task owner
     * @param status task status to match ({@code TODO}, {@code IN_PROGRESS}, or {@code DONE})
     * @return matching tasks ordered by {@code createdAt} descending
     */
    List<Task> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, Task.Status status);

    /**
     * Finds a single task only when it belongs to the given user.
     *
     * @param id     primary key of the task
     * @param userId Keycloak subject ID of the expected owner
     * @return the task when found and owned by the user, otherwise empty
     */
    Optional<Task> findByIdAndUserId(UUID id, String userId);

    /**
     * Counts how many tasks a user has in a given status.
     *
     * @param userId Keycloak subject ID of the task owner
     * @param status task status to count
     * @return number of matching tasks
     */
    long countByUserIdAndStatus(String userId, Task.Status status);

    /**
     * Returns tasks scheduled for a specific day with the given status.
     *
     * @param userId        Keycloak subject ID of the task owner
     * @param scheduledDate calendar day the task is committed to
     * @param status        task status to match
     * @return matching tasks ordered by {@code createdAt} descending
     */
    List<Task> findByUserIdAndScheduledDateAndStatusOrderByCreatedAtDesc(
            String userId, LocalDate scheduledDate, Task.Status status);

    /**
     * Returns weekly challenge tasks for the given week anchor (Monday).
     *
     * @param userId               Keycloak subject ID of the task owner
     * @param weeklyChallengeWeek  Monday date identifying the challenge week
     * @return flagged weekly challenge tasks
     */
    List<Task> findByUserIdAndWeeklyChallengeTrueAndWeeklyChallengeWeek(
            String userId, LocalDate weeklyChallengeWeek);

    /**
     * Counts tasks completed within an instant range (typically one local day).
     *
     * @param userId Keycloak subject ID of the task owner
     * @param start  inclusive lower bound on {@code completedAt}
     * @param end    exclusive upper bound on {@code completedAt}
     * @return number of completed tasks in the range
     */
    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.userId = :userId
              AND t.status = com.succeshub.appdomain.model.Task.Status.DONE
              AND t.completedAt >= :start
              AND t.completedAt < :end
            """)
    long countCompletedInRange(
            @Param("userId") String userId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    /**
     * Finds weekly challenge flags from prior weeks that should be cleared.
     *
     * @param userId Keycloak subject ID of the task owner
     * @param monday Monday anchor of the current challenge week
     * @return stale weekly challenge tasks
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND t.weeklyChallenge = true
              AND t.weeklyChallengeWeek IS NOT NULL
              AND t.weeklyChallengeWeek < :monday
            """)
    List<Task> findStaleWeeklyChallenges(@Param("userId") String userId, @Param("monday") LocalDate monday);

    /**
     * Returns TODO tasks eligible for weekly challenge assignment.
     *
     * @param userId     Keycloak subject ID of the task owner
     * @param status     task status (typically {@code TODO})
     * @param difficulty minimum difficulty threshold
     * @return eligible tasks ordered by {@code createdAt} descending
     */
    List<Task> findByUserIdAndStatusAndDifficultyGreaterThanEqualAndWeeklyChallengeFalseOrderByCreatedAtDesc(
            String userId, Task.Status status, int difficulty);
}
