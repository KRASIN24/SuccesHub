package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.LootBoxContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Deletes loot-box content rows for every box owned by the user.
     *
     * @param userId Keycloak subject ID
     * @return rows deleted
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            delete from LootBoxContent c
            where c.lootBox.id in (
                select b.id from UserLootBox b where b.userId = :userId
            )
            """)
    int deleteByLootBoxUserId(@Param("userId") String userId);
}
