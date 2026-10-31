package com.successhub.appdomain.entity;

import com.successhub.appdomain.enums.RewardRarity;
import com.successhub.appdomain.enums.RewardType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "reward_definition", schema = "successhub")
@Getter
@Setter
@NoArgsConstructor
public class RewardDefinition {

    @Id
    private UUID id;

    @Column(name = "key", unique = true, nullable = false)
    private String key;

    @Column(name = "label", nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RewardType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false)
    private RewardRarity rarity;

    @Column(name = "icon", nullable = false)
    private String icon;

    /** Human-readable effect for functional rewards (SHIELD / XP_BOOST); null for cosmetics. */
    @Column(name = "effect")
    private String effect;
}
