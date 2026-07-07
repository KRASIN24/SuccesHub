package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_profile", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_id", unique = true, nullable = false)
    private String keycloakId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "current_xp", nullable = false)
    private int currentXp = 0;

    @Column(name = "next_level_xp", nullable = false)
    private int nextLevelXp = 1000;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "global_rank", nullable = false)
    private int globalRank = 9999;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "streak_tier")
    private StreakTier streakTier = StreakTier.NONE;

    @Column(name = "streak_shields", nullable = false)
    private int streakShields = 0;

    @Column(name = "daily_xp_earned", nullable = false)
    private int dailyXpEarned = 0;

    @Column(name = "daily_xp_date")
    private LocalDate dailyXpDate;

    @Column(name = "lifetime_xp", nullable = false)
    private int lifetimeXp = 0;

    @Column(name = "first_task_completed_today", nullable = false)
    private boolean firstTaskCompletedToday = false;

    @Column(name = "last_processed_day")
    private LocalDate lastProcessedDay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum StreakTier { NONE, BRONZE, SILVER, GOLD }

    public static StreakTier tierForStreak(int streak) {
        if (streak >= 100) {
            return StreakTier.GOLD;
        }
        if (streak >= 30) {
            return StreakTier.SILVER;
        }
        if (streak >= 7) {
            return StreakTier.BRONZE;
        }
        return StreakTier.NONE;
    }

    /**
     * Award XP and automatically level up when the threshold is reached.
     * Each level requires 20% more XP than the previous one.
     */
    public void addXp(int amount) {
        if (amount <= 0) {
            return;
        }
        this.lifetimeXp += amount;
        this.currentXp += amount;
        while (this.currentXp >= this.nextLevelXp) {
            this.currentXp -= this.nextLevelXp;
            this.level++;
            this.nextLevelXp = (int) (this.nextLevelXp * 1.2);
        }
    }
}
