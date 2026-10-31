package com.successhub.appdomain.service;

import com.successhub.appdomain.dto.gamification.RewardEventDto;
import com.successhub.appdomain.entity.Task;

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
