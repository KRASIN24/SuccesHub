package com.successhub.appdomain.dto.gamification;

import com.successhub.appdomain.dto.ProfileDto;
import com.successhub.appdomain.dto.TaskDto;

public record TaskCompletionDto(
        TaskDto.Response task,
        RewardEventDto reward,
        ProfileDto updatedProfile
) {}
