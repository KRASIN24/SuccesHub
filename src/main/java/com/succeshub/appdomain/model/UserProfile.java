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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Award XP and automatically level up when the threshold is reached.
     * Each level requires 20% more XP than the previous one.
     */
    public void addXp(int amount) {
        this.currentXp += amount;
        while (this.currentXp >= this.nextLevelXp) {
            this.currentXp -= this.nextLevelXp;
            this.level++;
            this.nextLevelXp = (int) (this.nextLevelXp * 1.2);
        }
    }
}
