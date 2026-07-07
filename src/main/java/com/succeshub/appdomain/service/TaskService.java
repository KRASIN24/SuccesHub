package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.TaskDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.TaskCompletionDto;
import com.succeshub.appdomain.model.Task;

import java.util.List;
import java.util.UUID;

/**
 * Application service for user-owned protocol tasks on the Strategy Canvas and dashboard.
 */
public interface TaskService {

    /**
     * Returns every task belonging to the user, newest first.
     *
     * @param userId Keycloak subject ID of the task owner
     * @return all tasks as API responses
     */
    List<TaskDto.Response> getTasks(String userId);

    /**
     * Returns tasks filtered by workflow status.
     *
     * @param userId Keycloak subject ID of the task owner
     * @param status status to match
     * @return matching tasks as API responses
     */
    List<TaskDto.Response> getTasksByStatus(String userId, Task.Status status);

    /**
     * Creates a new task for the user, optionally assigning a category column.
     *
     * @param userId Keycloak subject ID of the task owner
     * @param req    creation payload from the client
     * @return persisted task as an API response
     */
    TaskDto.Response create(String userId, TaskDto.CreateRequest req);

    /**
     * Updates an existing task owned by the user.
     *
     * @param userId Keycloak subject ID of the task owner
     * @param id     primary key of the task to update
     * @param req    update payload from the client
     * @return updated task as an API response
     */
    TaskDto.Response update(String userId, UUID id, TaskDto.UpdateRequest req);

    /**
     * Marks a task as done and runs the gamification engine when the category grants XP.
     *
     * @param userId Keycloak subject ID of the task owner
     * @param id     primary key of the task to complete
     * @return task, reward payload, and updated profile; idempotent when already done or XP was previously awarded
     */
    TaskCompletionDto complete(String userId, UUID id);

    /**
     * Commits tasks to today's daily ritual schedule.
     *
     * @param userId  Keycloak subject ID of the task owner
     * @param taskIds task IDs to schedule for today
     */
    void scheduleTasks(String userId, List<UUID> taskIds);

    /**
     * Deletes a task owned by the user.
     *
     * @param userId Keycloak subject ID of the task owner
     * @param id     primary key of the task to delete
     */
    void delete(String userId, UUID id);
}
