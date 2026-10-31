package com.successhub.appdomain.dto.gamification;

/** Request body for manually granting (summoning) a pending box of a given type. */
public record GrantBoxRequest(String boxType) {}
