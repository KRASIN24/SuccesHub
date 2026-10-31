package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.enums.GoalStatus;
import com.successhub.appdomain.enums.RewardType;
import com.successhub.appdomain.enums.RewardRarity;
import com.successhub.appdomain.dto.ProfileDto;
import com.successhub.appdomain.dto.gamification.BossDamageDto;
import com.successhub.appdomain.dto.gamification.DevCampaignStateRequest;
import com.successhub.appdomain.dto.gamification.DevCombatStateRequest;
import com.successhub.appdomain.dto.gamification.DevInventoryGrantResultDto;
import com.successhub.appdomain.dto.gamification.InventoryItemDto;
import com.successhub.appdomain.dto.gamification.RewardItemDto;
import com.successhub.appdomain.entity.Goal;
import com.successhub.appdomain.entity.RewardDefinition;
import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.repository.GoalCategorySubscriptionRepository;
import com.successhub.appdomain.repository.GoalRepository;
import com.successhub.appdomain.repository.RewardDefinitionRepository;
import com.successhub.appdomain.repository.TaskCategoryRepository;
import com.successhub.appdomain.repository.TaskRepository;
import com.successhub.appdomain.repository.UserLootBoxRepository;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.appdomain.service.BossDamageService;
import com.successhub.appdomain.service.LootBoxService;
import com.successhub.appdomain.service.UserProfileService;
import com.successhub.appdomain.service.gamification.GamificationTimeUtil;
import com.successhub.appdomain.config.GamificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevGrantServiceImplTest {

    private static final String USER = "user-sub";

    @Mock UserProfileService profileService;
    @Mock UserProfileRepository profileRepository;
    @Mock LootBoxService lootBoxService;
    @Mock RewardDefinitionRepository rewardDefinitionRepository;
    @Mock GoalRepository goalRepository;
    @Mock GoalCategorySubscriptionRepository subscriptionRepository;
    @Mock TaskCategoryRepository categoryRepository;
    @Mock TaskRepository taskRepository;
    @Mock UserLootBoxRepository lootBoxRepository;
    @Mock BossDamageService bossDamageService;
    @Mock GamificationTimeUtil timeUtil;

    private GamificationProperties properties;
    private DevGrantServiceImpl service;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        properties = new GamificationProperties();
        properties.setDailyXpCap(300);
        service = new DevGrantServiceImpl(
                profileService,
                profileRepository,
                lootBoxService,
                rewardDefinitionRepository,
                goalRepository,
                subscriptionRepository,
                categoryRepository,
                taskRepository,
                lootBoxRepository,
                bossDamageService,
                timeUtil,
                properties);

        profile = new UserProfile();
        profile.setKeycloakId(USER);
        profile.setLevel(1);
        profile.setCurrentXp(0);
        profile.setNextLevelXp(1000);
    }

    @Test
    void addXp_delegatesToProfileService() {
        // Arrange
        ProfileDto dto = new ProfileDto(USER, "A", 2, 100, 1200, 0, 1, true, "en-US");
        when(profileService.addXp(USER, 50)).thenReturn(dto);

        // Act
        ProfileDto result = service.addXp(USER, 50);

        // Assert
        assertEquals(2, result.level());
        verify(profileService).addXp(USER, 50);
    }

    @Test
    void addXp_rejectsNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> service.addXp(USER, 0));
    }

    @Test
    void setLevel_recomputesThresholdAndClearsXp() {
        // Arrange
        when(profileService.requireProfile(USER)).thenReturn(profile);
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileService.mapToDto(any(UserProfile.class))).thenAnswer(inv -> {
            UserProfile p = inv.getArgument(0);
            return new ProfileDto(USER, null, p.getLevel(), p.getCurrentXp(), p.getNextLevelXp(), 0, 1, true, "en-US");
        });

        // Act
        ProfileDto result = service.setLevel(USER, 3);

        // Assert
        assertEquals(3, result.level());
        assertEquals(0, result.currentXp());
        assertEquals(DevGrantServiceImpl.nextLevelXpFor(3), result.nextLevelXp());
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getLevel());
        assertEquals(0, captor.getValue().getCurrentXp());
    }

    @Test
    void grantInventory_returnsStackTotal() {
        // Arrange
        RewardItemDto reward = new RewardItemDto(
                UUID.randomUUID(), "streak_shield", "Shield", "SHIELD", "COMMON", "shield", null);
        InventoryItemDto item = new InventoryItemDto(UUID.randomUUID(), reward, 4, false);
        when(lootBoxService.grantInventoryItem(USER, "streak_shield", 2)).thenReturn(item);

        // Act
        DevInventoryGrantResultDto result = service.grantInventory(USER, "streak_shield", 2);

        // Assert
        assertEquals(2, result.quantityGranted());
        assertEquals(4, result.quantityTotal());
        assertTrue(result.message().contains("Shield"));
    }

    @Test
    void setCombatState_updatesShieldsAndFlags() {
        // Arrange
        when(profileService.requireProfile(USER)).thenReturn(profile);
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileService.mapToDto(any(UserProfile.class))).thenReturn(
                new ProfileDto(USER, null, 1, 0, 1000, 0, 1, true, "en-US"));

        // Act
        service.setCombatState(USER, new DevCombatStateRequest(3, true, false));

        // Assert
        assertEquals(3, profile.getStreakShields());
        assertTrue(profile.isPendingDoubleStrike());
        assertEquals(false, profile.isPendingSurgeToken());
    }

    @Test
    void setCampaignState_clampsPendingToRemainingHp() {
        // Arrange
        UUID goalId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setId(goalId);
        goal.setUserId(USER);
        goal.setName("Boss");
        goal.setTargetValue(100);
        goal.setCurrentProgress(90);
        goal.setPendingDamage(0);
        goal.setStatus(GoalStatus.ACTIVE);
        when(goalRepository.findByIdAndUserId(goalId, USER)).thenReturn(Optional.of(goal));
        when(goalRepository.save(goal)).thenReturn(goal);
        when(subscriptionRepository.findByGoal_IdAndUserId(goalId, USER)).thenReturn(List.of());

        // Act — request more pending than remaining HP (10)
        var result = service.setCampaignState(USER, goalId, new DevCampaignStateRequest(null, 50.0));

        // Assert
        assertEquals(10.0, goal.getPendingDamage(), 0.001);
        assertEquals(10.0, result.pendingDamage(), 0.001);
    }

    @Test
    void strike_delegatesToBossDamageService() {
        // Arrange
        UUID goalId = UUID.randomUUID();
        BossDamageDto dto = new BossDamageDto(goalId, "Boss", 10, 0, true, 50);
        when(bossDamageService.strike(USER, goalId)).thenReturn(dto);

        // Act
        BossDamageDto result = service.strike(USER, goalId);

        // Assert
        assertTrue(result.goalCompleted());
        verify(bossDamageService).strike(USER, goalId);
    }

    @Test
    void diagnostics_reportsDailyRemainingWithoutMutating() {
        // Arrange
        profile.setDailyXpDate(LocalDate.of(2026, 10, 31));
        profile.setDailyXpEarned(100);
        profile.setStreakShields(2);
        profile.setPendingDoubleStrike(true);
        when(profileService.requireProfile(USER)).thenReturn(profile);
        when(timeUtil.today()).thenReturn(LocalDate.of(2026, 10, 31));
        when(timeUtil.getDayOffset()).thenReturn(0);
        when(lootBoxRepository.findByUserIdAndStatusOrderByCreatedAtDesc(eq(USER), any()))
                .thenReturn(List.of());
        when(goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(USER, GoalStatus.ACTIVE))
                .thenReturn(List.of());

        // Act
        var diag = service.diagnostics(USER);

        // Assert
        assertEquals(100, diag.dailyXpEarned());
        assertEquals(200, diag.dailyXpRemaining());
        assertEquals(2, diag.streakShields());
        assertTrue(diag.pendingDoubleStrike());
    }

    @Test
    void nextLevelXpFor_matchesAddXpGrowth() {
        assertEquals(1000, DevGrantServiceImpl.nextLevelXpFor(1));
        assertEquals(1200, DevGrantServiceImpl.nextLevelXpFor(2));
        assertEquals(1440, DevGrantServiceImpl.nextLevelXpFor(3));
    }

    @Test
    void listRewards_mapsCatalog() {
        // Arrange
        RewardDefinition def = new RewardDefinition();
        def.setKey("frame_gold");
        def.setLabel("Gold Frame");
        def.setType(RewardType.FRAME);
        def.setRarity(RewardRarity.RARE);
        def.setIcon("frame");
        when(rewardDefinitionRepository.findAll()).thenReturn(List.of(def));

        // Act
        var list = service.listRewards();

        // Assert
        assertEquals(1, list.size());
        assertEquals("frame_gold", list.getFirst().key());
        assertEquals("FRAME", list.getFirst().type());
    }
}
