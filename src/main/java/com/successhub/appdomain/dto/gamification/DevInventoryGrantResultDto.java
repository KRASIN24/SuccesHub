package com.successhub.appdomain.dto.gamification;

/** Result of granting inventory stacks via Dev tools. */
public record DevInventoryGrantResultDto(String rewardKey, int quantityGranted, int quantityTotal, String message) {}
