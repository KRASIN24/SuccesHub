package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.gamification.GamificationDto.RewardEventDto;
import com.succeshub.appdomain.model.Task;

/**
 * Central orchestrator for task-completion rewards: XP, boss damage, achievements, and loot triggers.
 */
public interface GamificationEngine {

    /**
     * Processes all gamification side effects for a newly completed task.
     *
     * @param userId Keycloak subject ID
     * @param task   completed task entity
     * @return reward payload for frontend animations
     */
    RewardEventDto processTaskCompletion(String userId, Task task);
}
