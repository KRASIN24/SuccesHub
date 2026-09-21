package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.model.UserInventory;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.RewardDefinitionRepository;
import com.succeshub.appdomain.repository.UserInventoryRepository;
import com.succeshub.appdomain.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Default streak outcome and shield accounting. Inventory shields and banked
 * {@link UserProfile#getStreakShields()} are separate pools — spending inventory does not
 * also decrement the banked counter (and vice versa).
 *
 * <p>V1: grace period and day boundaries use server timezone ({@code ZoneId.systemDefault()}).
 * User timezone support deferred to V2.
 */
@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private static final String STREAK_SHIELD_KEY = "STREAK_SHIELD";

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
        if (consumeShield(profile.getKeycloakId(), profile)) {
            return true;
        }
        profile.setCurrentStreak(0);
        profile.setStreakTier(UserProfile.StreakTier.NONE);
        return false;
    }

    @Override
    @Transactional
    public boolean consumeShield(String userId, UserProfile profile) {
        var shieldDef = rewardDefinitionRepository.findByKey(STREAK_SHIELD_KEY);
        if (shieldDef.isPresent()) {
            var inv = inventoryRepository.findByUserIdAndRewardDefinitionId(userId, shieldDef.get().getId());
            if (inv.isPresent() && inv.get().getQuantity() > 0) {
                decrementStack(inv.get());
                return true;
            }
        }
        if (profile.getStreakShields() > 0) {
            profile.setStreakShields(profile.getStreakShields() - 1);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public int shieldsAvailable(String userId, UserProfile profile) {
        int inventory = rewardDefinitionRepository.findByKey(STREAK_SHIELD_KEY)
                .flatMap(def -> inventoryRepository.findByUserIdAndRewardDefinitionId(userId, def.getId()))
                .map(UserInventory::getQuantity)
                .orElse(0);
        return profile.getStreakShields() + inventory;
    }

    private void decrementStack(UserInventory item) {
        int remaining = Math.max(0, item.getQuantity() - 1);
        if (remaining == 0) {
            inventoryRepository.delete(item);
        } else {
            item.setQuantity(remaining);
            inventoryRepository.save(item);
        }
    }
}
