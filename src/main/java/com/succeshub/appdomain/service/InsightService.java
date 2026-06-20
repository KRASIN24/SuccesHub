package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.gamification.GamificationDto.WeeklyInsightDto;

/**
 * Weekly competence metrics derived from structured XP event data.
 */
public interface InsightService {

    /**
     * Compares current-week activity to the prior week for the goals insight panel.
     *
     * @param userId Keycloak subject ID
     * @return comparative metrics and summary text
     */
    WeeklyInsightDto getWeeklyInsights(String userId);
}
