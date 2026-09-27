package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.gamification.GamificationDto.CloseDayResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DailyStatusDto;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.StreakDayOverrideRepository;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.appdomain.service.AchievementEvaluator;
import com.succeshub.appdomain.service.LootBoxService;
import com.succeshub.appdomain.service.StreakService;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import com.succeshub.config.GamificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRitualServiceImplTest {

    @Mock UserProfileService userProfileService;
    @Mock UserProfileRepository profileRepository;
    @Mock TaskRepository taskRepository;
    @Mock StreakDayOverrideRepository overrideRepository;
    @Mock StreakService streakService;
    @Mock LootBoxService lootBoxService;
    @Mock AchievementEvaluator achievementEvaluator;
    @Mock GamificationTimeUtil timeUtil;
    @Mock TaskMapper taskMapper;

    private GamificationProperties properties;
    private DailyRitualServiceImpl service;

    private UserProfile profile;
    private LocalDate today;
    private LocalDate yesterday;

    @BeforeEach
    void setUp() {
        properties = new GamificationProperties();
        properties.setMinTasksForQualifyingDay(1);
        properties.setDailyXpCap(300);
        service = new DailyRitualServiceImpl(
                userProfileService, profileRepository, taskRepository, overrideRepository,
                streakService, lootBoxService, achievementEvaluator, properties, timeUtil, taskMapper);

        today = LocalDate.of(2026, 9, 22);
        yesterday = today.minusDays(1);

        profile = new UserProfile();
        profile.setKeycloakId("user-1");
        profile.setCurrentStreak(4);
        profile.setStreakTier(UserProfile.StreakTier.NONE);
        profile.setStreakShields(0);
        profile.setDailyXpEarned(50);
        profile.setLastProcessedDay(yesterday.minusDays(1));
    }

    @Test
    void closePendingDays_processesYesterdayOnceAndAdvancesCursor() {
        // Arrange
        when(userProfileService.requireProfile("user-1")).thenReturn(profile);
        when(timeUtil.today()).thenReturn(today);
        when(timeUtil.startOfDay(yesterday)).thenReturn(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
        when(timeUtil.endOfDay(yesterday)).thenReturn(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1));
        when(timeUtil.startOfDay(today)).thenReturn(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        when(timeUtil.endOfDay(today)).thenReturn(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1));
        when(overrideRepository.existsByUserIdAndDay("user-1", yesterday)).thenReturn(false);
        when(taskRepository.countCompletedInRange(eq("user-1"), any(), any())).thenReturn(1L);
        when(streakService.applyDayOutcome(eq(profile), eq(yesterday), eq(true))).thenAnswer(inv -> {
            profile.setCurrentStreak(5);
            return false;
        });
        when(lootBoxService.checkStreakMilestone("user-1", 5)).thenReturn(null);
        when(achievementEvaluator.evaluateAndUnlock(eq("user-1"), eq(profile), anyLong())).thenReturn(List.of());

        // Act
        CloseDayResultDto result = service.closePendingDays("user-1");

        // Assert
        assertFalse(result.alreadyClosed());
        assertEquals(4, result.streakBefore());
        assertEquals(5, result.streakAfter());
        assertEquals(yesterday, profile.getLastProcessedDay());
        verify(streakService).applyDayOutcome(profile, yesterday, true);
        verify(profileRepository).save(profile);
    }

    @Test
    void closePendingDays_alreadyClosed_skipsStreakLogic() {
        // Arrange
        profile.setLastProcessedDay(yesterday);
        when(userProfileService.requireProfile("user-1")).thenReturn(profile);
        when(timeUtil.today()).thenReturn(today);

        // Act
        CloseDayResultDto result = service.closePendingDays("user-1");

        // Assert
        assertTrue(result.alreadyClosed());
        assertEquals(4, result.streakBefore());
        assertEquals(4, result.streakAfter());
        verify(streakService, never()).applyDayOutcome(any(), any(), anyBoolean());
        verify(profileRepository).save(profile);
    }

    @Test
    void celebrateDay_setsLastCelebratedDay() {
        // Arrange
        profile.setLastProcessedDay(yesterday);
        when(userProfileService.requireProfile("user-1")).thenReturn(profile);
        when(timeUtil.today()).thenReturn(today);

        // Act
        service.celebrateDay("user-1");

        // Assert
        assertEquals(today, profile.getLastCelebratedDay());
    }

    @Test
    void unsealToday_clearsCelebrateWhenSameDay() {
        // Arrange
        profile.setLastCelebratedDay(today);
        when(userProfileService.requireProfile("user-1")).thenReturn(profile);
        when(timeUtil.today()).thenReturn(today);

        // Act
        boolean unsealed = service.unsealToday("user-1");

        // Assert
        assertTrue(unsealed);
        assertEquals(null, profile.getLastCelebratedDay());
        verify(profileRepository).save(profile);
    }

    @Test
    void getDailyStatus_marksDaySealedWhenCelebratedAndQualifying() {
        // Arrange
        profile.setLastProcessedDay(yesterday);
        profile.setLastCelebratedDay(today);
        when(userProfileService.requireProfile("user-1")).thenReturn(profile);
        when(timeUtil.today()).thenReturn(today);
        when(timeUtil.mondayOfWeek(today)).thenReturn(today.with(java.time.DayOfWeek.MONDAY));
        when(timeUtil.startOfDay(today)).thenReturn(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        when(timeUtil.endOfDay(today)).thenReturn(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1));
        when(taskRepository.findByUserIdAndScheduledDateAndStatusOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(taskRepository.findByUserIdAndWeeklyChallengeTrueAndWeeklyChallengeWeek(any(), any()))
                .thenReturn(List.of());
        when(taskRepository.countCompletedInRange(eq("user-1"), any(), any())).thenReturn(2L);

        // Act
        DailyStatusDto status = service.getDailyStatus("user-1");

        // Assert
        assertTrue(status.daySealed());
        assertFalse(status.pendingCelebrations());
    }
}
