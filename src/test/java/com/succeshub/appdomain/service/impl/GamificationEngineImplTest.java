package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.gamification.GamificationDto.BossDamageDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.RewardEventDto;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.model.XpEvent;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.appdomain.repository.XpEventRepository;
import com.succeshub.appdomain.service.AchievementEvaluator;
import com.succeshub.appdomain.service.BossDamageService;
import com.succeshub.appdomain.service.LootBoxService;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import com.succeshub.appdomain.service.gamification.XpCalculator;
import com.succeshub.config.GamificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GamificationEngineImplTest {

    @Mock UserProfileService userProfileService;
    @Mock UserProfileRepository profileRepository;
    @Mock XpEventRepository xpEventRepository;
    @Mock TaskRepository taskRepository;
    @Mock XpCalculator xpCalculator;
    @Mock GamificationTimeUtil timeUtil;
    @Mock BossDamageService bossDamageService;
    @Mock AchievementEvaluator achievementEvaluator;
    @Mock LootBoxService lootBoxService;

    private GamificationProperties properties;
    private GamificationEngineImpl engine;

    private UUID taskId;
    private Task task;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        properties = new GamificationProperties();
        properties.setDailyXpCap(300);
        engine = new GamificationEngineImpl(
                userProfileService, profileRepository, xpEventRepository, taskRepository,
                xpCalculator, properties, timeUtil, bossDamageService, achievementEvaluator, lootBoxService);

        taskId = UUID.randomUUID();
        task = new Task();
        task.setId(taskId);
        task.setDifficulty(3);
        task.setWeeklyChallenge(false);

        profile = new UserProfile();
        profile.setKeycloakId("user-1");
        profile.setLevel(1);
        profile.setCurrentXp(0);
        profile.setNextLevelXp(1000);
        profile.setDailyXpEarned(0);
        profile.setLifetimeXp(0);
    }

    @Test
    void processTaskCompletion_returnsNullWhenXpEventAlreadyExists() {
        // Arrange
        when(xpEventRepository.existsByUserIdAndTaskId("user-1", taskId)).thenReturn(true);

        // Act
        RewardEventDto result = engine.processTaskCompletion("user-1", task);

        // Assert
        assertNull(result);
        verify(userProfileService, never()).requireProfile(any());
    }

    @Test
    void processTaskCompletion_awardsBaseXpAndPersistsEvent() {
        // Arrange
        stubHappyPath(new XpCalculator.XpResult(60, 0, 10, 0, 0, 70, false));
        when(bossDamageService.applyDamage("user-1", task)).thenReturn(null);
        when(timeUtil.today()).thenReturn(LocalDate.of(2026, 9, 22));
        when(taskRepository.countCompletedInRange(eq("user-1"), any(), any())).thenReturn(1L);
        when(achievementEvaluator.evaluateAndUnlock(eq("user-1"), eq(profile), eq(1L))).thenReturn(List.of());

        // Act
        RewardEventDto result = engine.processTaskCompletion("user-1", task);

        // Assert
        assertEquals(70, result.xp().totalXp());
        assertEquals(70, profile.getDailyXpEarned());
        assertTrue(profile.isFirstTaskCompletedToday());
        assertFalse(result.leveledUp());

        ArgumentCaptor<XpEvent> eventCaptor = ArgumentCaptor.forClass(XpEvent.class);
        verify(xpEventRepository).save(eventCaptor.capture());
        assertEquals(70, eventCaptor.getValue().getTotalXp());
        assertEquals(taskId, eventCaptor.getValue().getTaskId());
        verify(profileRepository).save(profile);
    }

    @Test
    void processTaskCompletion_doubleStrike_doublesRawXpThenClearsFlag() {
        // Arrange
        profile.setPendingDoubleStrike(true);
        stubHappyPath(new XpCalculator.XpResult(50, 0, 0, 0, 0, 50, false));
        when(xpCalculator.applyDailyCap(100, 0)).thenReturn(100);
        when(bossDamageService.applyDamage("user-1", task)).thenReturn(null);
        when(timeUtil.today()).thenReturn(LocalDate.of(2026, 9, 22));
        when(taskRepository.countCompletedInRange(eq("user-1"), any(), any())).thenReturn(1L);
        when(achievementEvaluator.evaluateAndUnlock(any(), any(), anyLong())).thenReturn(List.of());

        // Act
        RewardEventDto result = engine.processTaskCompletion("user-1", task);

        // Assert
        assertEquals(100, result.xp().totalXp());
        assertFalse(profile.isPendingDoubleStrike());
        verify(xpCalculator).applyDailyCap(100, 0);
    }

    @Test
    void processTaskCompletion_surgeToken_addsFiftyAfterCapAndClearsFlag() {
        // Arrange
        profile.setPendingSurgeToken(true);
        profile.setDailyXpEarned(280);
        stubHappyPath(new XpCalculator.XpResult(60, 0, 0, 0, 0, 60, false));
        when(xpCalculator.applyDailyCap(60, 280)).thenReturn(20);
        when(bossDamageService.applyDamage("user-1", task)).thenReturn(null);
        when(timeUtil.today()).thenReturn(LocalDate.of(2026, 9, 22));
        when(taskRepository.countCompletedInRange(eq("user-1"), any(), any())).thenReturn(1L);
        when(achievementEvaluator.evaluateAndUnlock(any(), any(), anyLong())).thenReturn(List.of());

        // Act
        RewardEventDto result = engine.processTaskCompletion("user-1", task);

        // Assert — 20 from capped raw + 50 surge (surge ignores remaining cap)
        assertEquals(70, result.xp().totalXp());
        assertEquals(350, profile.getDailyXpEarned());
        assertFalse(profile.isPendingSurgeToken());
    }

    @Test
    void processTaskCompletion_bossKill_addsGoalXpReward() {
        // Arrange
        stubHappyPath(new XpCalculator.XpResult(40, 0, 0, 0, 0, 40, false));
        when(xpCalculator.applyDailyCap(40, 0)).thenReturn(40);
        BossDamageDto boss = new BossDamageDto(UUID.randomUUID(), "Hydra", 10, 0, true, 200);
        when(bossDamageService.applyDamage("user-1", task)).thenReturn(boss);
        when(timeUtil.today()).thenReturn(LocalDate.of(2026, 9, 22));
        when(taskRepository.countCompletedInRange(eq("user-1"), any(), any())).thenReturn(1L);
        when(achievementEvaluator.evaluateAndUnlock(any(), any(), anyLong())).thenReturn(List.of());

        // Act
        RewardEventDto result = engine.processTaskCompletion("user-1", task);

        // Assert
        assertEquals(40, result.xp().totalXp());
        assertEquals(240, profile.getDailyXpEarned());
        assertTrue(result.bossDamage().goalCompleted());
    }

    private void stubHappyPath(XpCalculator.XpResult xpResult) {
        when(xpEventRepository.existsByUserIdAndTaskId("user-1", taskId)).thenReturn(false);
        when(userProfileService.requireProfile("user-1")).thenReturn(profile);
        when(timeUtil.today()).thenReturn(LocalDate.of(2026, 9, 22));
        when(xpCalculator.calculateForCompletion(eq(task), eq(profile), anyBoolean())).thenReturn(xpResult);
        lenient().when(xpCalculator.applyDailyCap(xpResult.totalXp(), profile.getDailyXpEarned()))
                .thenReturn(xpResult.totalXp());
    }
}
