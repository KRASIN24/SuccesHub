package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.UserLootBox;
import org.springframework.data.domain.Pageable;
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
     * Returns opened loot boxes for a user, most recently opened first.
     *
     * @param userId Keycloak subject ID
     * @param status box status filter (typically OPENED)
     * @param pageable pagination / limit
     * @return matching boxes
     */
    List<UserLootBox> findByUserIdAndStatusOrderByOpenedAtDesc(
            String userId, UserLootBox.Status status, Pageable pageable);

    /**
     * Finds a loot box owned by the given user.
     *
     * @param id     loot box ID
     * @param userId expected owner
     * @return box when found and owned
     */
    Optional<UserLootBox> findByIdAndUserId(UUID id, String userId);
}
