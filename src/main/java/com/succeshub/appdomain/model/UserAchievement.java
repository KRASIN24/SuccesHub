package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_achievement", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private AchievementDefinition achievement;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt = Instant.now();
}
