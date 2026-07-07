package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.RewardDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Global reward definition catalog for loot boxes and inventory.
 */
public interface RewardDefinitionRepository extends JpaRepository<RewardDefinition, UUID> {

    /**
     * Finds a reward by its stable key.
     *
     * @param key unique reward key
     * @return matching definition if present
     */
    Optional<RewardDefinition> findByKey(String key);
}
