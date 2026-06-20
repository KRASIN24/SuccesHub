package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "achievement_definition", schema = "succeshub")
@Getter
@Setter
@NoArgsConstructor
public class AchievementDefinition {

    @Id
    private UUID id;

    @Column(name = "key", unique = true, nullable = false)
    private String key;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "icon", nullable = false)
    private String icon;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "xp_required")
    private int xpRequired = 0;

    @Column(name = "tasks_required")
    private int tasksRequired = 0;

    @Column(name = "streak_required")
    private int streakRequired = 0;

    @Column(name = "goals_required")
    private int goalsRequired = 0;
}
