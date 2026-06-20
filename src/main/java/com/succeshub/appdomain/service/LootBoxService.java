package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.gamification.GamificationDto.InventoryItemDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.LootBoxDto;
import com.succeshub.appdomain.model.UserLootBox;

import java.util.List;
import java.util.UUID;

/**
 * Earn-only loot box lifecycle: grant, open, and inventory management.
 */
public interface LootBoxService {

    /**
     * Grants a pending loot box to the user.
     *
     * @param userId Keycloak subject ID
     * @param source earn trigger source
     * @return created loot box ID
     */
    UUID grantLootBox(String userId, UserLootBox.Source source);

    /**
     * Opens a pending loot box and rolls rewards into inventory.
     *
     * @param userId Keycloak subject ID
     * @param lootBoxId box to open
     * @return opened box with contents
     */
    LootBoxDto openLootBox(String userId, UUID lootBoxId);

    /**
     * Lists pending loot boxes for the user.
     *
     * @param userId Keycloak subject ID
     * @return unopened boxes
     */
    List<LootBoxDto> getPendingLootBoxes(String userId);

    /**
     * Returns the user's inventory stacks.
     *
     * @param userId Keycloak subject ID
     * @return inventory items
     */
    List<InventoryItemDto> getInventory(String userId);

    /**
     * Awards streak milestone loot when applicable.
     *
     * @param userId Keycloak subject ID
     * @param streak current streak after increment
     * @return loot box ID if granted, else {@code null}
     */
    UUID checkStreakMilestone(String userId, int streak);
}
