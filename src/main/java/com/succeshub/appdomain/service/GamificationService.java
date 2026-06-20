package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.gamification.GamificationDto.XpPreviewDto;

/**
 * Read-only gamification calculations exposed to the API (XP preview, forecasts).
 */
public interface GamificationService {

    /**
     * Estimates XP for a hypothetical task completion without persisting state.
     *
     * @param userId           Keycloak subject ID
     * @param difficulty       task difficulty 1–5
     * @param durationMinutes  estimated duration
     * @param priority         task priority 1–3
     * @param weeklyChallenge  whether the task is flagged as weekly challenge
     * @return breakdown with daily cap applied
     */
    XpPreviewDto previewXp(String userId, int difficulty, int durationMinutes, int priority, boolean weeklyChallenge);
}
