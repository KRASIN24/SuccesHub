package com.successhub.appdomain.dto.gamification;

/** Request to change the streak by a signed delta (dev/testing tool). */
public record AdjustStreakRequest(int delta) {}
