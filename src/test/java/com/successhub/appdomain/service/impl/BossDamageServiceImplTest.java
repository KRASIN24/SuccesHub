package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.enums.GoalStatus;
import com.successhub.appdomain.dto.gamification.BossDamageDto;
import com.successhub.appdomain.entity.Goal;
import com.successhub.appdomain.entity.Task;
import com.successhub.appdomain.repository.GoalRepository;
import com.successhub.appdomain.service.UserProfileService;
import com.successhub.appdomain.config.GamificationProperties;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BossDamageServiceImplTest {

    @Mock GoalRepository goalRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock UserProfileService profileService;

    private GamificationProperties properties;
    private BossDamageServiceImpl service;

    private UUID goalId;
    private Goal goal;
    private Task task;

    @BeforeEach
    void setUp() {
        properties = new GamificationProperties();
        properties.setBossDamageFactor(2);
        service = new BossDamageServiceImpl(goalRepository, properties, eventPublisher, profileService);

        goalId = UUID.randomUUID();
        goal = new Goal();
        goal.setId(goalId);
        goal.setUserId("user-1");
        goal.setName("Hydra");
        goal.setTargetValue(100);
        goal.setCurrentProgress(0);
        goal.setPendingDamage(0);
        goal.setXpReward(200);
        goal.setStatus(GoalStatus.ACTIVE);

        task = new Task();
        task.setDifficulty(5);
        task.setGoal(goal);
    }

    @Test
    void applyDamage_returnsNullWhenTaskHasNoGoal() {
        task.setGoal(null);

        BossDamageDto dto = service.applyDamage("user-1", task);

        assertNull(dto);
    }

    @Test
    void applyDamage_returnsNullWhenGoalAlreadyCompleted() {
        goal.setStatus(GoalStatus.COMPLETED);
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.of(goal));

        BossDamageDto dto = service.applyDamage("user-1", task);

        assertNull(dto);
    }

    @Test
    void applyDamage_throwsWhenGoalMissingForUser() {
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.applyDamage("user-1", task));
    }

    @Test
    void applyDamage_banksDifficultyTimesFactorWithoutChangingHp() {
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.of(goal));

        BossDamageDto dto = service.applyDamage("user-1", task);

        assertNotNull(dto);
        assertEquals(10.0, dto.progressDelta());
        assertEquals(100, dto.healthRemaining());
        assertFalse(dto.goalCompleted());
        assertEquals(0, dto.goalXpReward());
        assertEquals(0.0, goal.getCurrentProgress());
        assertEquals(10.0, goal.getPendingDamage());
    }

    @Test
    void strike_appliesPendingAndClearsBank() {
        goal.setPendingDamage(25);
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.of(goal));

        BossDamageDto dto = service.strike("user-1", goalId);

        assertEquals(25.0, dto.progressDelta());
        assertEquals(75, dto.healthRemaining());
        assertFalse(dto.goalCompleted());
        assertEquals(25.0, goal.getCurrentProgress());
        assertEquals(0.0, goal.getPendingDamage());
    }

    @Test
    void strike_completesGoalWhenPendingDepletesHp() {
        goal.setCurrentProgress(95);
        goal.setPendingDamage(10);
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.of(goal));

        BossDamageDto dto = service.strike("user-1", goalId);

        assertTrue(dto.goalCompleted());
        assertEquals(200, dto.goalXpReward());
        assertEquals(GoalStatus.COMPLETED, goal.getStatus());
        assertEquals("Slain", goal.getSlainLabel());
        assertNotNull(goal.getCompletedAt());
        assertEquals(0, dto.healthRemaining());
        assertEquals(0.0, goal.getPendingDamage());
        verify(profileService).addXp("user-1", 200);

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        assertEquals(GoalStatus.COMPLETED, captor.getValue().getStatus());
    }

    @Test
    void strike_throwsWhenNoPendingDamage() {
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.of(goal));

        assertThrows(IllegalStateException.class, () -> service.strike("user-1", goalId));
    }
}
