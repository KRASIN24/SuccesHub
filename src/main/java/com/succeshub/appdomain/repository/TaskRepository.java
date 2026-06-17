package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
