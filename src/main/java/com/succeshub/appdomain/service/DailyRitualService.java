package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.gamification.GamificationDto.CloseDayResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DailyStatusDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.ForecastDto;

/**
 * Daily ritual orchestration: lazy day-close, status, and optional celebration.
 */
public interface DailyRitualService {

    /**
     * Idempotently closes all unprocessed days through yesterday. Primary streak integrity path.
     *
     * @param userId Keycloak subject ID
     * @return close summary with streak delta and earned loot
     */
    CloseDayResultDto closePendingDays(String userId);

    /**
     * Runs lazy close then returns dashboard daily status.
     *
     * @param userId Keycloak subject ID
     * @return today's mission summary
     */
    DailyStatusDto getDailyStatus(String userId);

    /**
     * Returns anticipation data for tomorrow's bonuses and weekly challenges.
     *
     * @param userId Keycloak subject ID
     * @return forecast widget payload
     */
    ForecastDto getForecast(String userId);
}
