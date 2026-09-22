package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.UserInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User-owned inventory items from loot rewards.
 */
public interface UserInventoryRepository extends JpaRepository<UserInventory, UUID> {

    /**
     * Returns all inventory rows for a user in stable id order
     * (avoids Collection UI reshuffling after equip refreshes).
     *
     * @param userId Keycloak subject ID
     * @return inventory items ordered by id ascending
     */
    List<UserInventory> findByUserIdOrderByIdAsc(String userId);

    /**
     * Finds an inventory stack for a specific reward type.
     *
     * @param userId              Keycloak subject ID
     * @param rewardDefinitionId  reward definition ID
     * @return matching stack if present
     */
    Optional<UserInventory> findByUserIdAndRewardDefinitionId(String userId, UUID rewardDefinitionId);
}
