package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.TaskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence access for Kanban-style {@link TaskCategory} columns owned by a user.
 */
public interface TaskCategoryRepository extends JpaRepository<TaskCategory, UUID> {

    /**
     * Returns all categories for a user in board display order.
     *
     * @param userId Keycloak subject ID of the category owner
     * @return categories ordered by {@code sortOrder} ascending
     */
    List<TaskCategory> findByUserIdOrderBySortOrderAsc(String userId);
}
