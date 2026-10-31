package com.successhub.appdomain.dto.gamification;

/** Request body for granting catalog rewards into inventory. */
public record DevInventoryGrantRequest(String rewardKey, int quantity) {}
