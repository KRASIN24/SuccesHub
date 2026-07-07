package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.gamification.GamificationDto.BossDamageDto;
import com.succeshub.appdomain.model.Task;

/**
 * Applies boss (goal) damage when tasks are linked to active goals.
 */
public interface BossDamageService {

    /**
     * Chips progress off a linked goal when a task is completed.
     *
     * @param userId Keycloak subject ID
     * @param task   completed task with optional goal link
     * @return damage summary, or {@code null} when no linked goal
     */
    BossDamageDto applyDamage(String userId, Task task);
}
