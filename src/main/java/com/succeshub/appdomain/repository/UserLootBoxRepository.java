package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.UserLootBox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-user loot box persistence.
 */
public interface UserLootBoxRepository extends JpaRepository<UserLootBox, UUID> {

    /**
     * Returns pending loot boxes for a user, newest first.
     *
     * @param userId Keycloak subject ID
     * @param status box status filter
     * @return matching boxes
     */
    List<UserLootBox> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, UserLootBox.Status status);

    /**
     * Finds a loot box owned by the given user.
     *
     * @param id     loot box ID
     * @param userId expected owner
     * @return box when found and owned
     */
    Optional<UserLootBox> findByIdAndUserId(UUID id, String userId);
}
