package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.AchievementDto;

import java.util.List;

/**
 * Application service for achievement definitions and per-user unlock state.
 */
public interface AchievementService {

    /**
     * Returns every achievement definition with lock state for the given user.
     *
     * @param userId Keycloak subject ID of the player
     * @return all achievements, indicating which are unlocked and when
     */
    List<AchievementDto> getAchievements(String userId);
}
