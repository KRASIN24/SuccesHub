package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.StreakDayOverrideRepository;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserInventoryRepository;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.appdomain.service.StreakService;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import com.succeshub.config.GamificationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakManagementServiceImplTest {

    @Mock UserProfileService userProfileService;
    @Mock UserProfileRepository profileRepository;
    @Mock TaskRepository taskRepository;
    @Mock StreakDayOverrideRepository overrideRepository;
    @Mock UserInventoryRepository inventoryRepository;
    @Mock StreakService streakService;
    @Mock GamificationProperties properties;
    @Mock GamificationTimeUtil timeUtil;

    @InjectMocks StreakManagementServiceImpl service;

    @Test
    void setStreakPinsDailySettlementCursor() {
        LocalDate today = LocalDate.of(2026, 9, 23);
        UserProfile profile = profile("kc-1");
        when(userProfileService.requireProfile("kc-1")).thenReturn(profile);
        when(timeUtil.today()).thenReturn(today);

        service.setStreak("kc-1", 30);

        assertEquals(30, profile.getCurrentStreak());
        assertEquals(UserProfile.StreakTier.SILVER, profile.getStreakTier());
        assertEquals(today.minusDays(1), profile.getLastProcessedDay());
        assertEquals(today, profile.getLastActiveDate());
    }

    @Test
    void resetStreakPinsDailySettlementCursor() {
        LocalDate today = LocalDate.of(2026, 9, 23);
        UserProfile profile = profile("kc-1");
        profile.setCurrentStreak(30);
        profile.setStreakTier(UserProfile.StreakTier.SILVER);
        when(userProfileService.requireProfile("kc-1")).thenReturn(profile);
        when(timeUtil.today()).thenReturn(today);

        service.resetStreak("kc-1");

        assertEquals(0, profile.getCurrentStreak());
        assertEquals(UserProfile.StreakTier.NONE, profile.getStreakTier());
        assertEquals(today.minusDays(1), profile.getLastProcessedDay());
    }

    @Test
    void shieldingProcessedDayDoesNotAdvanceSettlementCursor() {
        LocalDate today = LocalDate.of(2026, 9, 23);
        LocalDate shieldedDay = LocalDate.of(2026, 9, 20);
        UserProfile profile = profile("kc-1");
        profile.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        profile.setCurrentStreak(5);
        profile.setLastProcessedDay(shieldedDay);
        profile.setLastActiveDate(shieldedDay.minusDays(1));

        when(userProfileService.requireProfile("kc-1")).thenReturn(profile);
        when(timeUtil.today()).thenReturn(today);
        when(timeUtil.zone()).thenReturn(java.time.ZoneOffset.UTC);
        when(properties.getMinTasksForQualifyingDay()).thenReturn(3);
        when(streakService.consumeShield("kc-1", profile)).thenReturn(true);
        when(taskRepository.countCompletedInRange(
                org.mockito.ArgumentMatchers.eq("kc-1"), any(), any())).thenReturn(0L);

        service.shieldDay("kc-1", shieldedDay);

        assertEquals(6, profile.getCurrentStreak());
        assertEquals(shieldedDay, profile.getLastProcessedDay());
        assertEquals(shieldedDay, profile.getLastActiveDate());
    }

    private UserProfile profile(String keycloakId) {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId(keycloakId);
        return profile;
    }
}
