package com.successhub.appdomain.dto.gamification;

import com.successhub.appdomain.dto.AchievementDto;
import java.util.List;
import java.util.UUID;

public record RewardEventDto(
        XpBreakdownDto xp,
        boolean leveledUp,
        int previousLevel,
        int newLevel,
        boolean variableBonusTriggered,
        BossDamageDto bossDamage,
        List<AchievementDto> achievementsUnlocked,
        UUID lootBoxEarned
) {}
