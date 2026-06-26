package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "reward_definition", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class RewardDefinition {

    public enum Type { SHIELD, XP_BOOST, TITLE, FRAME, BADGE }
    public enum Rarity { COMMON, RARE, LEGENDARY }

    @Id
    private UUID id;

    @Column(name = "key", unique = true, nullable = false)
    private String key;

    @Column(name = "label", nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false)
    private Rarity rarity;

    @Column(name = "icon", nullable = false)
    private String icon;

    /** Human-readable effect for functional rewards (SHIELD / XP_BOOST); null for cosmetics. */
    @Column(name = "effect")
    private String effect;
}
