package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for user-owned {@link Goal} entities used on the Boss Battle page.
 */
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    /**
     * Returns goals for a user filtered by lifecycle status.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @param status goal status to match ({@code ACTIVE}, {@code COMPLETED}, or {@code ABANDONED})
     * @return matching goals ordered by {@code createdAt} descending
     */
    List<Goal> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, Goal.Status status);

    /**
     * Finds a single goal only when it belongs to the given user.
     *
     * @param id     primary key of the goal
     * @param userId Keycloak subject ID of the expected owner
     * @return the goal when found and owned by the user, otherwise empty
     */
    Optional<Goal> findByIdAndUserId(UUID id, String userId);

    /**
     * Counts all goals for a user regardless of status.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @return total number of goals
     */
    @Query("SELECT COUNT(g) FROM Goal g WHERE g.userId = :userId")
    long countTotal(@Param("userId") String userId);

    /**
     * Counts goals the user has completed.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @return number of goals with status {@code COMPLETED}
     */
    @Query("SELECT COUNT(g) FROM Goal g WHERE g.userId = :userId AND g.status = 'COMPLETED'")
    long countCompleted(@Param("userId") String userId);

    /**
     * Counts goals for a user in a given lifecycle status.
     *
     * @param userId Keycloak subject ID of the goal owner
     * @param status goal status to count
     * @return number of matching goals
     */
    long countByUserIdAndStatus(String userId, Goal.Status status);
}
