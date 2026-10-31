package com.successhub.appdomain.dto.gamification;

/** Request body for setting or clearing the virtual day offset (dev/testing). */
public record DevClockRequest(Integer dayOffset, Boolean clear) {}
