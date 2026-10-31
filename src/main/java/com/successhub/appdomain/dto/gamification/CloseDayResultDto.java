package com.successhub.appdomain.dto.gamification;

import com.successhub.appdomain.dto.AchievementDto;
import java.util.List;
import java.util.UUID;

public record CloseDayResultDto(
        int streakBefore,
        int streakAfter,
        String streakTier,
        List<AchievementDto> achievementsUnlocked,
        List<UUID> lootBoxesEarned,
        boolean alreadyClosed
) {}
