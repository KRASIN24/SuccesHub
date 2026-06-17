package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.TaskCategoryDto;

import java.util.List;
import java.util.UUID;

/**
 * Application service for Kanban columns that group a user's tasks.
 */
public interface TaskCategoryService {

    /**
     * Returns all category columns for the user in board display order.
     *
     * @param userId Keycloak subject ID of the category owner
     * @return ordered list of categories
     */
    List<TaskCategoryDto.Response> getCategories(String userId);

    /**
     * Creates a new task category column for the user.
     *
     * @param userId Keycloak subject ID of the category owner
     * @param req    creation payload from the client
     * @return persisted category as an API response
     */
    TaskCategoryDto.Response create(String userId, TaskCategoryDto.CreateRequest req);

    /**
     * Updates an existing category owned by the user.
     *
     * @param userId Keycloak subject ID of the category owner
     * @param id     primary key of the category to update
     * @param req    update payload from the client
     * @return updated category as an API response
     */
    TaskCategoryDto.Response update(String userId, UUID id, TaskCategoryDto.UpdateRequest req);

    /**
     * Deletes a category owned by the user.
     *
     * @param userId Keycloak subject ID of the category owner
     * @param id     primary key of the category to delete
     */
    void delete(String userId, UUID id);
}
