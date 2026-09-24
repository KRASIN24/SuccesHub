package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.model.UserLootBox;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.LootBoxContentRepository;
import com.succeshub.appdomain.repository.RewardDefinitionRepository;
import com.succeshub.appdomain.repository.StreakDayOverrideRepository;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserInventoryRepository;
import com.succeshub.appdomain.repository.UserLootBoxRepository;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import com.succeshub.config.GamificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LootBoxServiceImplStreakTest {

    @Mock UserLootBoxRepository lootBoxRepository;
    @Mock LootBoxContentRepository contentRepository;
    @Mock RewardDefinitionRepository rewardDefinitionRepository;
    @Mock UserInventoryRepository inventoryRepository;
    @Mock TaskRepository taskRepository;
    @Mock StreakDayOverrideRepository overrideRepository;
    @Mock GamificationTimeUtil timeUtil;

    private GamificationProperties properties;
    private LootBoxServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new GamificationProperties();
        properties.setMinTasksForQualifyingDay(1);
        properties.setWeeklyLootMinQualifyingDays(3);
        service = new LootBoxServiceImpl(
                lootBoxRepository, contentRepository, rewardDefinitionRepository,
                inventoryRepository, taskRepository, overrideRepository, properties, timeUtil);

        lenient().when(lootBoxRepository.save(any(UserLootBox.class))).thenAnswer(inv -> {
            UserLootBox box = inv.getArgument(0);
            if (box.getId() == null) {
                box.setId(UUID.randomUUID());
            }
            return box;
        });
    }

    @Test
    void checkStreakMilestone_grantsAtSevenAndThirtyAndHundredOnly() {
        // Arrange — save stubbed in setUp

        // Act & Assert
        assertNotNull(service.checkStreakMilestone("user-1", 7));
        assertNotNull(service.checkStreakMilestone("user-1", 30));
        assertNotNull(service.checkStreakMilestone("user-1", 100));
        assertNull(service.checkStreakMilestone("user-1", 6));
        assertNull(service.checkStreakMilestone("user-1", 8));
        assertNull(service.checkStreakMilestone("user-1", 99));
    }

    @Test
    void checkWeeklyLoot_skipsWhenAlreadyGrantedForWeek() {
        // Arrange
        UserProfile profile = new UserProfile();
        LocalDate monday = LocalDate.of(2026, 9, 21);
        profile.setLastWeeklyLootWeek(monday);

        // Act
        UUID boxId = service.checkWeeklyLoot("user-1", profile, monday);

        // Assert
        assertNull(boxId);
        verify(lootBoxRepository, never()).save(any());
    }

    @Test
    void checkWeeklyLoot_grantsWhenEnoughQualifyingDays() {
        // Arrange
        UserProfile profile = new UserProfile();
        LocalDate monday = LocalDate.of(2026, 9, 21);
        ZoneId zone = ZoneId.systemDefault();

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            var start = day.atStartOfDay(zone).toInstant();
            var end = day.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1);
            when(overrideRepository.existsByUserIdAndDay("user-1", day)).thenReturn(false);
            when(timeUtil.startOfDay(day)).thenReturn(start);
            when(timeUtil.endOfDay(day)).thenReturn(end);
            when(taskRepository.countCompletedInRange("user-1", start, end))
                    .thenReturn(i < 3 ? 1L : 0L);
        }

        // Act
        UUID boxId = service.checkWeeklyLoot("user-1", profile, monday);

        // Assert
        assertNotNull(boxId);
        assertEquals(monday, profile.getLastWeeklyLootWeek());

        ArgumentCaptor<UserLootBox> captor = ArgumentCaptor.forClass(UserLootBox.class);
        verify(lootBoxRepository).save(captor.capture());
        assertEquals(UserLootBox.Source.WEEKLY_RESET, captor.getValue().getSource());
    }

    @Test
    void checkWeeklyLoot_returnsNullWhenBelowThreshold() {
        // Arrange
        UserProfile profile = new UserProfile();
        LocalDate monday = LocalDate.of(2026, 9, 21);
        ZoneId zone = ZoneId.systemDefault();

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            when(overrideRepository.existsByUserIdAndDay("user-1", day)).thenReturn(false);
            when(timeUtil.startOfDay(day)).thenReturn(day.atStartOfDay(zone).toInstant());
            when(timeUtil.endOfDay(day)).thenReturn(day.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1));
        }
        when(taskRepository.countCompletedInRange(any(), any(), any())).thenReturn(0L);

        // Act
        UUID boxId = service.checkWeeklyLoot("user-1", profile, monday);

        // Assert
        assertNull(boxId);
        assertNull(profile.getLastWeeklyLootWeek());
    }
}
