package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "loot_box_content", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class LootBoxContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loot_box_id", nullable = false)
    private UserLootBox lootBox;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reward_definition_id", nullable = false)
    private RewardDefinition rewardDefinition;
}
