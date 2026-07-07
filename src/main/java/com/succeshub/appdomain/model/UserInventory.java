package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_inventory", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class UserInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reward_definition_id", nullable = false)
    private RewardDefinition rewardDefinition;

    @Column(name = "quantity", nullable = false)
    private int quantity = 0;

    @Column(name = "equipped", nullable = false)
    private boolean equipped = false;
}
