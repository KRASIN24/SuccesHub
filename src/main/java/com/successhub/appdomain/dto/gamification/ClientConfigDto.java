package com.successhub.appdomain.dto.gamification;

/**
 * Client-safe gamification feature flags and clock snapshot for the SPA
 * (loot summon, streak testing tools, and virtual day offset).
 *
 * @param enableLootDevGrants when true, testing tools and grant APIs are available
 * @param effectiveToday      ISO date used as gamification "today" (may include day offset)
 * @param dayOffset           days added to the real calendar for testing (0 = none)
 */
public record ClientConfigDto(boolean enableLootDevGrants, String effectiveToday, int dayOffset) {}
