package com.successhub.appdomain.service;

import com.successhub.appdomain.dto.GoalDto;
import com.successhub.appdomain.dto.GoalSubscriptionDto;
import com.successhub.appdomain.dto.TaskDto;

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
     * Returns a single goal owned by the user.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @param id     goal primary key
     * @return goal as an API response
     */
    GoalDto.Response getById(String userId, UUID id);

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

    /**
     * Lists tasks linked to a campaign via {@code goalId}.
     *
     * @param userId Keycloak subject ID
     * @param goalId campaign ID
     * @return linked tasks
     */
    List<TaskDto.Response> getLinkedTasks(String userId, UUID goalId);

    /**
     * Attaches an existing task to this campaign.
     *
     * @param userId Keycloak subject ID
     * @param goalId campaign ID
     * @param taskId task ID
     * @return updated task
     */
    TaskDto.Response attachTask(String userId, UUID goalId, UUID taskId);

    /**
     * Detaches a task from this campaign (clears {@code goalId}).
     *
     * @param userId Keycloak subject ID
     * @param goalId campaign ID
     * @param taskId task ID
     * @return updated task
     */
    TaskDto.Response detachTask(String userId, UUID goalId, UUID taskId);

    /**
     * Lists category subscriptions for a campaign.
     *
     * @param userId Keycloak subject ID
     * @param goalId campaign ID
     * @return subscriptions
     */
    List<GoalSubscriptionDto.Response> listSubscriptions(String userId, UUID goalId);

    /**
     * Subscribes a category to a campaign (exclusive per category).
     * Fills open tasks in that category that have no goal yet.
     *
     * @param userId Keycloak subject ID
     * @param goalId campaign ID
     * @param req    category to subscribe
     * @return created subscription
     */
    GoalSubscriptionDto.Response subscribeCategory(String userId, UUID goalId, GoalSubscriptionDto.SubscribeRequest req);

    /**
     * Removes a category subscription from a campaign.
     *
     * @param userId     Keycloak subject ID
     * @param goalId     campaign ID
     * @param categoryId category to unsubscribe
     */
    void unsubscribeCategory(String userId, UUID goalId, UUID categoryId);
}
