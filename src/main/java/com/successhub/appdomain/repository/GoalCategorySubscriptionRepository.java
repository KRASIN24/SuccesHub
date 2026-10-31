package com.successhub.appdomain.repository;

import com.successhub.appdomain.entity.GoalCategorySubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for live category→campaign subscriptions.
 */
public interface GoalCategorySubscriptionRepository extends JpaRepository<GoalCategorySubscription, UUID> {

    /**
     * Lists subscriptions for a campaign owned by the user.
     *
     * @param goalId campaign ID
     * @param userId Keycloak subject ID
     * @return subscriptions for that goal
     */
    List<GoalCategorySubscription> findByGoal_IdAndUserId(UUID goalId, String userId);

    /**
     * Finds the exclusive subscription for a category, if any.
     *
     * @param categoryId task category ID
     * @return subscription when the category is linked to a campaign
     */
    Optional<GoalCategorySubscription> findByCategory_Id(UUID categoryId);

    /**
     * Finds a subscription by category and owner.
     *
     * @param categoryId task category ID
     * @param userId     Keycloak subject ID
     * @return matching subscription
     */
    Optional<GoalCategorySubscription> findByCategory_IdAndUserId(UUID categoryId, String userId);

    /**
     * Finds a subscription row for unsubscribe.
     *
     * @param goalId     campaign ID
     * @param categoryId category ID
     * @param userId     Keycloak subject ID
     * @return matching subscription
     */
    Optional<GoalCategorySubscription> findByGoal_IdAndCategory_IdAndUserId(
            UUID goalId, UUID categoryId, String userId);

    /**
     * Deletes all subscriptions for a user (account deletion cascade).
     *
     * @param userId Keycloak subject ID
     */
    void deleteByUserId(String userId);
}
