package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Live link: tasks created in {@link #category} inherit {@link #goal} until unsubscribed.
 * Each category may subscribe to at most one goal.
 */
@Entity
@Table(name = "goal_category_subscription", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class GoalCategorySubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, unique = true)
    private TaskCategory category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
