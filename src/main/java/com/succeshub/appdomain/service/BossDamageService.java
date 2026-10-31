package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.gamification.GamificationDto.BossDamageDto;
import com.succeshub.appdomain.model.Task;

import java.util.UUID;

/**
 * Banks boss (goal) damage from linked task completions and releases it on Strike.
 */
public interface BossDamageService {

    /**
     * Banks damage into the linked goal's pending pool when a task is completed.
     * Does not change HP; call {@link #strike} to release pending damage.
     *
     * @param userId Keycloak subject ID
     * @param task   completed task with optional goal link
     * @return damage summary (banked delta), or {@code null} when no linked goal
     */
    BossDamageDto applyDamage(String userId, Task task);

    /**
     * Releases all pending damage into the goal's progress (capped at remaining HP).
     * Completes the goal and awards XP when HP reaches zero.
     *
     * @param userId Keycloak subject ID
     * @param goalId campaign to strike
     * @return damage applied this strike
     * @throws jakarta.persistence.EntityNotFoundException if the goal is missing
     * @throws IllegalStateException if the campaign is inactive or has no pending damage
     */
    BossDamageDto strike(String userId, UUID goalId);
}
