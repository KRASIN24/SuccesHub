package com.successhub.appdomain.dto.gamification;

/** Result of bulk-subscribing categories to a campaign. */
public record DevSubscribeAllResultDto(int subscribed, int alreadyLinked, int moved, String message) {}
