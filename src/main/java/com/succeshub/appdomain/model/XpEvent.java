package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "xp_event", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class XpEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "base_xp", nullable = false)
    private int baseXp;

    @Column(name = "streak_bonus", nullable = false)
    private int streakBonus;

    @Column(name = "first_task_bonus", nullable = false)
    private int firstTaskBonus;

    @Column(name = "variable_bonus", nullable = false)
    private int variableBonus;

    @Column(name = "challenge_bonus", nullable = false)
    private int challengeBonus;

    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    @Column(name = "difficulty", nullable = false)
    private int difficulty;

    @Column(name = "is_weekly_challenge", nullable = false)
    private boolean weeklyChallenge;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
