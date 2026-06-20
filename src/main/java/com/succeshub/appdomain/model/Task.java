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
@Table(name = "task", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    public enum Status { TODO, IN_PROGRESS, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TaskCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    private Goal goal;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward = 0;

    @Column(name = "difficulty", nullable = false)
    private int difficulty = 3;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Column(name = "priority", nullable = false)
    private int priority = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.TODO;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "is_weekly_challenge", nullable = false)
    private boolean weeklyChallenge = false;

    @Column(name = "weekly_challenge_week")
    private LocalDate weeklyChallengeWeek;

    @Column(name = "meta_label")
    private String metaLabel;

    @Column(name = "meta_type")
    private String metaType;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "xp_awarded", nullable = false)
    private boolean xpAwarded = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
