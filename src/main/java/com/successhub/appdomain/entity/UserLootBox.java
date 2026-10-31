package com.successhub.appdomain.entity;

import com.successhub.appdomain.enums.LootBoxOpenStatus;
import com.successhub.appdomain.enums.LootBoxSource;
import com.successhub.appdomain.enums.LootBoxType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_loot_box", schema = "successhub")
@Getter
@Setter
@NoArgsConstructor
public class UserLootBox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private LootBoxSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "box_type", nullable = false)
    private LootBoxType boxType = LootBoxType.ARCANE_ORB;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LootBoxOpenStatus status = LootBoxOpenStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "opened_at")
    private Instant openedAt;
}
