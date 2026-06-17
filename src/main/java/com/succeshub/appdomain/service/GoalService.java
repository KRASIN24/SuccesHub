package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.GoalDto;

import java.util.List;
import java.util.UUID;

/**
 * Application service for user goals, progress tracking, and completion rewards.
 */
public interface GoalService {

    /**
     * Returns all active goals for the user, newest first.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @return active goals as API responses
     */
    List<GoalDto.Response> getActiveGoals(String userId);

    /**
     * Returns all completed goals for the user, newest first.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @return completed goals as API responses
     */
    List<GoalDto.Response> getCompletedGoals(String userId);

    /**
     * Returns aggregate counts and completion percentage for the user's goals.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @return summary totals and completion percent
     */
    GoalDto.SummaryResponse getSummary(String userId);

    /**
     * Creates a new goal for the user.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @param req    creation payload from the client
     * @return persisted goal as an API response
     */
    GoalDto.Response create(String userId, GoalDto.CreateRequest req);

    /**
     * Updates an existing goal owned by the user.
     * Awards XP when status transitions to completed.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @param id     primary key of the goal to update
     * @param req    update payload from the client
     * @return updated goal as an API response
     */
    GoalDto.Response update(String userId, UUID id, GoalDto.UpdateRequest req);

    /**
     * Deletes a goal owned by the user.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @param id     primary key of the goal to delete
     */
    void delete(String userId, UUID id);
}
