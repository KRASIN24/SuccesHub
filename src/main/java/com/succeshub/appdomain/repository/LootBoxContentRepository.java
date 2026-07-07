package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.LootBoxContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Contents revealed from opened loot boxes.
 */
public interface LootBoxContentRepository extends JpaRepository<LootBoxContent, UUID> {

    /**
     * Returns all rewards rolled for a loot box.
     *
     * @param lootBoxId loot box primary key
     * @return content rows
     */
    List<LootBoxContent> findByLootBox_Id(UUID lootBoxId);
}
