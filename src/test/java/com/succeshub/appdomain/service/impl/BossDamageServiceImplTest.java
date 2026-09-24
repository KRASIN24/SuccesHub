package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.gamification.GamificationDto.BossDamageDto;
import com.succeshub.appdomain.model.Goal;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.config.GamificationProperties;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private GamificationProperties properties;
    private BossDamageServiceImpl service;

    private UUID goalId;
    private Goal goal;
    private Task task;

    @BeforeEach
    void setUp() {
        properties = new GamificationProperties();
        properties.setBossDamageFactor(2);
        service = new BossDamageServiceImpl(goalRepository, properties);

        goalId = UUID.randomUUID();
        goal = new Goal();
        goal.setId(goalId);
        goal.setUserId("user-1");
        goal.setName("Hydra");
        goal.setTargetValue(100);
        goal.setCurrentProgress(0);
        goal.setXpReward(200);
        goal.setStatus(Goal.Status.ACTIVE);

        task = new Task();
        task.setDifficulty(5);
        task.setGoal(goal);
    }

    @Test
    void applyDamage_returnsNullWhenTaskHasNoGoal() {
        // Arrange
        task.setGoal(null);

        // Act
        BossDamageDto dto = service.applyDamage("user-1", task);

        // Assert
        assertNull(dto);
    }

    @Test
    void applyDamage_returnsNullWhenGoalAlreadyCompleted() {
        // Arrange
        goal.setStatus(Goal.Status.COMPLETED);
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.of(goal));

        // Act
        BossDamageDto dto = service.applyDamage("user-1", task);

        // Assert
        assertNull(dto);
    }

    @Test
    void applyDamage_throwsWhenGoalMissingForUser() {
        // Arrange
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> service.applyDamage("user-1", task));
    }

    @Test
    void applyDamage_appliesDifficultyTimesFactor() {
        // Arrange
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.of(goal));

        // Act
        BossDamageDto dto = service.applyDamage("user-1", task);

        // Assert
        assertNotNull(dto);
        assertEquals(10.0, dto.progressDelta());
        assertEquals(90, dto.healthRemaining());
        assertFalse(dto.goalCompleted());
        assertEquals(0, dto.goalXpReward());
        assertEquals(10.0, goal.getCurrentProgress());
    }

    @Test
    void applyDamage_completesGoalAndAwardsXpWhenHealthDepleted() {
        // Arrange
        goal.setCurrentProgress(95);
        when(goalRepository.findByIdAndUserId(goalId, "user-1")).thenReturn(Optional.of(goal));

        // Act
        BossDamageDto dto = service.applyDamage("user-1", task);

        // Assert
        assertTrue(dto.goalCompleted());
        assertEquals(200, dto.goalXpReward());
        assertEquals(Goal.Status.COMPLETED, goal.getStatus());
        assertEquals("Slain", goal.getSlainLabel());
        assertNotNull(goal.getCompletedAt());
        assertEquals(0, dto.healthRemaining());

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        assertEquals(Goal.Status.COMPLETED, captor.getValue().getStatus());
    }
}
