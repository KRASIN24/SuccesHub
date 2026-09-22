package com.succeshub.appdomain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A user-applied override on a single calendar day's streak outcome.
 *
 * <p>Currently used to record days that were manually protected with a streak shield card. A
 * {@code SHIELDED} day is treated as qualifying by the daily ritual so lazy day-close never
 * undoes a manual repair, and the profile streak calendar renders it distinctly.
 */
@Entity
@Table(
        name = "streak_day_override",
        schema = "succeshub",
        uniqueConstraints = @UniqueConstraint(name = "uq_streak_day_override_user_day", columnNames = {"user_id", "day"})
)
@Getter
@Setter
@NoArgsConstructor
public class StreakDayOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private Kind kind = Kind.SHIELDED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Type of manual day override. */
    public enum Kind {
        /** Day protected by a consumed streak shield card. */
        SHIELDED
    }
}
