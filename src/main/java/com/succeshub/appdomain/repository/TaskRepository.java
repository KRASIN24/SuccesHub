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
     * Returns the completion timestamps of all tasks completed within an instant range.
     * Used to bucket completions into calendar days for the streak calendar.
     *
     * @param userId Keycloak subject ID of the task owner
     * @param start  inclusive lower bound on {@code completedAt}
     * @param end    exclusive upper bound on {@code completedAt}
     * @return completion instants within the range
     */
    @Query("""
            SELECT t.completedAt FROM Task t
            WHERE t.userId = :userId
              AND t.status = com.succeshub.appdomain.model.Task.Status.DONE
              AND t.completedAt >= :start
              AND t.completedAt < :end
            """)
    List<Instant> findCompletedAtInRange(
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

    /**
     * Deletes every task owned by the user (account deletion cascade).
     *
     * @param userId Keycloak subject ID
     */
    void deleteByUserId(String userId);

    /**
     * Returns root or sibling tasks in a category ordered by sort order.
     *
     * @param userId       Keycloak subject ID of the task owner
     * @param categoryId   category column (nullable for uncategorized)
     * @param parentTaskId parent for subtasks, or null for roots
     * @return sibling tasks ordered by {@code sortOrder} ascending
     */
    List<Task> findByUserIdAndCategory_IdAndParentTask_IdOrderBySortOrderAsc(
            String userId, UUID categoryId, UUID parentTaskId);

    /**
     * Returns root tasks (no parent) in a category ordered by sort order.
     *
     * @param userId     Keycloak subject ID of the task owner
     * @param categoryId category column
     * @return root tasks ordered by {@code sortOrder} ascending
     */
    List<Task> findByUserIdAndCategory_IdAndParentTaskIsNullOrderBySortOrderAsc(
            String userId, UUID categoryId);

    /**
     * Counts children of a parent task (for max sort order assignment).
     *
     * @param parentId parent task ID
     * @return number of direct subtasks
     */
    long countByParentTask_Id(UUID parentId);

    /**
     * Counts root tasks in a category for the user.
     *
     * @param userId     Keycloak subject ID
     * @param categoryId category column
     * @return number of root tasks
     */
    long countByUserIdAndCategory_IdAndParentTaskIsNull(String userId, UUID categoryId);

    /**
     * Returns direct children of a parent task.
     *
     * @param parentId parent task ID
     * @return child tasks
     */
    List<Task> findByParentTask_Id(UUID parentId);

    /**
     * Returns incomplete tasks whose reminder is due and not yet notified.
     *
     * @param now cutoff instant
     * @return tasks ready for reminder notifications
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.reminderAt IS NOT NULL
              AND t.reminderAt <= :now
              AND t.reminderNotified = false
              AND t.status <> com.succeshub.appdomain.model.Task.Status.DONE
            """)
    List<Task> findDueReminders(@Param("now") Instant now);

    /**
     * Returns tasks linked to a campaign, newest first.
     *
     * @param userId Keycloak subject ID
     * @param goalId campaign ID
     * @return linked tasks
     */
    List<Task> findByUserIdAndGoal_IdOrderByCreatedAtDesc(String userId, UUID goalId);

    /**
     * Open (non-{@code status}) tasks in a category with no campaign link.
     *
     * @param userId     Keycloak subject ID
     * @param categoryId category ID
     * @param status     status to exclude (typically {@code DONE})
     * @return unlinked open tasks
     */
    List<Task> findByUserIdAndCategory_IdAndGoalIsNullAndStatusNot(
            String userId, UUID categoryId, Task.Status status);
}
