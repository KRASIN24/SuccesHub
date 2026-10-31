package com.successhub.appdomain.dto.gamification;

/** Request to set the streak to an absolute non-negative value (dev/testing tool). */
public record SetStreakRequest(int value) {}
