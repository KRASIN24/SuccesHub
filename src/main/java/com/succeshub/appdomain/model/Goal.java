package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "goal", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class Goal {

    public enum Status { ACTIVE, COMPLETED, ABANDONED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tier")
    private String tier;

    @Column(name = "target_description")
    private String targetDescription;

    @Column(name = "target_value", nullable = false)
    private double targetValue = 100.0;

    @Column(name = "current_progress", nullable = false)
    private double currentProgress = 0.0;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward = 0;

    @Column(name = "icon")
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "slain_label")
    private String slainLabel;

    @Column(name = "featured", nullable = false)
    private boolean featured = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Returns health remaining (100% = untouched, 0% = complete). */
    public int getHealthRemaining() {
        if (targetValue == 0) return 0;
        double done = currentProgress / targetValue;
        return Math.max(0, (int) Math.round((1.0 - done) * 100));
    }
}
