package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.model.RewardDefinition;
import com.succeshub.appdomain.model.UserInventory;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.RewardDefinitionRepository;
import com.succeshub.appdomain.repository.UserInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakServiceImplTest {

    @Mock UserInventoryRepository inventoryRepository;
    @Mock RewardDefinitionRepository rewardDefinitionRepository;

    @InjectMocks StreakServiceImpl streakService;

    private UserProfile profile;
    private RewardDefinition shieldDef;
    private UUID shieldDefId;

    @BeforeEach
    void setUp() {
        profile = new UserProfile();
        profile.setKeycloakId("user-1");
        profile.setCurrentStreak(5);
        profile.setStreakTier(UserProfile.StreakTier.NONE);
        profile.setStreakShields(0);

        shieldDefId = UUID.randomUUID();
        shieldDef = new RewardDefinition();
        shieldDef.setId(shieldDefId);
        shieldDef.setKey("STREAK_SHIELD");
    }

    @Test
    void applyDayOutcome_qualifying_incrementsStreakAndTier() {
        // Arrange — profile from setUp (streak 5)

        // Act
        boolean shielded = streakService.applyDayOutcome(profile, LocalDate.of(2026, 9, 22), true);

        // Assert
        assertFalse(shielded);
        assertEquals(6, profile.getCurrentStreak());
        assertEquals(LocalDate.of(2026, 9, 22), profile.getLastActiveDate());
        assertEquals(UserProfile.StreakTier.NONE, profile.getStreakTier());
    }

    @Test
    void applyDayOutcome_qualifying_atSeven_setsBronze() {
        // Arrange
        profile.setCurrentStreak(6);

        // Act
        streakService.applyDayOutcome(profile, LocalDate.of(2026, 9, 22), true);

        // Assert
        assertEquals(7, profile.getCurrentStreak());
        assertEquals(UserProfile.StreakTier.BRONZE, profile.getStreakTier());
    }

    @Test
    void applyDayOutcome_miss_consumesInventoryShieldFirst_withoutTouchingBank() {
        // Arrange
        profile.setStreakShields(2);
        UserInventory inv = inventory(2);
        when(rewardDefinitionRepository.findByKey("STREAK_SHIELD")).thenReturn(Optional.of(shieldDef));
        when(inventoryRepository.findByUserIdAndRewardDefinitionId("user-1", shieldDefId))
                .thenReturn(Optional.of(inv));

        // Act
        boolean shielded = streakService.applyDayOutcome(profile, LocalDate.of(2026, 9, 22), false);

        // Assert
        assertTrue(shielded);
        assertEquals(5, profile.getCurrentStreak());
        assertEquals(2, profile.getStreakShields());
        assertEquals(1, inv.getQuantity());
        verify(inventoryRepository).save(inv);
        verify(inventoryRepository, never()).delete(any());
    }

    @Test
    void applyDayOutcome_miss_fallsBackToBankedShield() {
        // Arrange
        profile.setStreakShields(1);
        when(rewardDefinitionRepository.findByKey("STREAK_SHIELD")).thenReturn(Optional.of(shieldDef));
        when(inventoryRepository.findByUserIdAndRewardDefinitionId("user-1", shieldDefId))
                .thenReturn(Optional.empty());

        // Act
        boolean shielded = streakService.applyDayOutcome(profile, LocalDate.of(2026, 9, 22), false);

        // Assert
        assertTrue(shielded);
        assertEquals(5, profile.getCurrentStreak());
        assertEquals(0, profile.getStreakShields());
    }

    @Test
    void applyDayOutcome_miss_withNoShields_resetsStreak() {
        // Arrange
        when(rewardDefinitionRepository.findByKey("STREAK_SHIELD")).thenReturn(Optional.empty());

        // Act
        boolean shielded = streakService.applyDayOutcome(profile, LocalDate.of(2026, 9, 22), false);

        // Assert
        assertFalse(shielded);
        assertEquals(0, profile.getCurrentStreak());
        assertEquals(UserProfile.StreakTier.NONE, profile.getStreakTier());
    }

    @Test
    void consumeShield_deletesInventoryStackWhenQuantityHitsZero() {
        // Arrange
        UserInventory inv = inventory(1);
        when(rewardDefinitionRepository.findByKey("STREAK_SHIELD")).thenReturn(Optional.of(shieldDef));
        when(inventoryRepository.findByUserIdAndRewardDefinitionId("user-1", shieldDefId))
                .thenReturn(Optional.of(inv));

        // Act
        boolean consumed = streakService.consumeShield("user-1", profile);

        // Assert
        assertTrue(consumed);
        verify(inventoryRepository).delete(inv);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void shieldsAvailable_sumsInventoryAndBankedPools() {
        // Arrange
        profile.setStreakShields(3);
        when(rewardDefinitionRepository.findByKey("STREAK_SHIELD")).thenReturn(Optional.of(shieldDef));
        when(inventoryRepository.findByUserIdAndRewardDefinitionId("user-1", shieldDefId))
                .thenReturn(Optional.of(inventory(2)));

        // Act
        int available = streakService.shieldsAvailable("user-1", profile);

        // Assert
        assertEquals(5, available);
    }

    private UserInventory inventory(int quantity) {
        UserInventory inv = new UserInventory();
        inv.setId(UUID.randomUUID());
        inv.setUserId("user-1");
        inv.setRewardDefinition(shieldDef);
        inv.setQuantity(quantity);
        return inv;
    }
}
