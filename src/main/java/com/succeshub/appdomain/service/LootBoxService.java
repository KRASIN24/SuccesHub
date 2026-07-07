package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.gamification.GamificationDto.BoxTypeDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.InventoryItemDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.LootBoxDto;
import com.succeshub.appdomain.model.LootBoxType;
import com.succeshub.appdomain.model.UserLootBox;

import java.util.List;
import java.util.UUID;

/**
 * Loot box lifecycle: grant, open, and inventory management.
 */
public interface LootBoxService {

    /**
     * Grants a pending loot box to the user, deriving the box type from the source.
     *
     * @param userId Keycloak subject ID
     * @param source earn trigger source
     * @return created loot box ID
     */
    UUID grantLootBox(String userId, UserLootBox.Source source);

    /**
     * Returns the static catalog of selectable box types with their rarity odds.
     *
     * @return ordered list of box type definitions
     */
    List<BoxTypeDto> getBoxTypes();

    /**
     * Grants a pending loot box of an explicit type (manual / earn-simulation).
     *
     * @param userId  Keycloak subject ID
     * @param boxType box type to grant
     * @return the newly created pending box
     */
    LootBoxDto grantBox(String userId, LootBoxType boxType);

    /**
     * Toggles the equipped state of a cosmetic inventory item (TITLE or FRAME).
     * Equipping one item unequips any other equipped item of the same type.
     *
     * @param userId      Keycloak subject ID
     * @param inventoryId inventory row to toggle
     * @return the updated inventory item
     */
    InventoryItemDto toggleEquip(String userId, UUID inventoryId);

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
     * Returns recently opened loot boxes with their rolled contents.
     *
     * @param userId Keycloak subject ID
     * @param limit  maximum number of opened boxes to return
     * @return opened boxes ordered by {@code openedAt} descending
     */
    List<LootBoxDto> getLootBoxHistory(String userId, int limit);

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
