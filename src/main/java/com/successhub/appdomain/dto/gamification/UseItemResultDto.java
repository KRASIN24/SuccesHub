package com.successhub.appdomain.dto.gamification;

import com.successhub.appdomain.dto.ProfileDto;

/** Result of consuming (using) a utility inventory card. */
public record UseItemResultDto(
        String rewardKey,
        String message,
        int quantityRemaining,
        ProfileDto updatedProfile
) {}
