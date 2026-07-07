package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.model.RewardDefinition;
import com.succeshub.appdomain.model.UserInventory;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.RewardDefinitionRepository;
import com.succeshub.appdomain.repository.UserInventoryRepository;
import com.succeshub.appdomain.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// V1: grace period and day boundaries use server timezone (ZoneId.systemDefault()).
// User timezone support deferred to V2 — do not "fix" by switching to user TZ without a profile timezone field.
@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private final UserInventoryRepository inventoryRepository;
    private final RewardDefinitionRepository rewardDefinitionRepository;

    @Override
    public boolean applyDayOutcome(UserProfile profile, LocalDate day, boolean qualifying) {
        if (qualifying) {
            profile.setCurrentStreak(profile.getCurrentStreak() + 1);
            profile.setStreakTier(UserProfile.tierForStreak(profile.getCurrentStreak()));
            profile.setLastActiveDate(day);
            return false;
        }
        return consumeShield(profile.getKeycloakId(), profile);
    }

    @Override
    @Transactional
    public boolean consumeShield(String userId, UserProfile profile) {
        var shieldDef = rewardDefinitionRepository.findByKey("STREAK_SHIELD");
        if (shieldDef.isPresent()) {
            var inv = inventoryRepository.findByUserIdAndRewardDefinitionId(userId, shieldDef.get().getId());
            if (inv.isPresent() && inv.get().getQuantity() > 0) {
                UserInventory stack = inv.get();
                stack.setQuantity(stack.getQuantity() - 1);
                inventoryRepository.save(stack);
                profile.setStreakShields(Math.max(0, profile.getStreakShields() - 1));
                return true;
            }
        }
        if (profile.getStreakShields() > 0) {
            profile.setStreakShields(profile.getStreakShields() - 1);
            return true;
        }
        profile.setCurrentStreak(0);
        profile.setStreakTier(UserProfile.StreakTier.NONE);
        return false;
    }
}
