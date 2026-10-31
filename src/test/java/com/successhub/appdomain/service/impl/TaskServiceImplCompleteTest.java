package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.enums.TaskStatus;
import com.successhub.appdomain.dto.ProfileDto;
import com.successhub.appdomain.dto.TaskDto;
import com.successhub.appdomain.dto.gamification.RewardEventDto;
import com.successhub.appdomain.dto.gamification.TaskCompletionDto;
import com.successhub.appdomain.entity.Task;
import com.successhub.appdomain.entity.TaskCategory;
import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.repository.GoalCategorySubscriptionRepository;
import com.successhub.appdomain.repository.GoalRepository;
import com.successhub.appdomain.repository.TaskCategoryRepository;
import com.successhub.appdomain.repository.TaskRepository;
import com.successhub.appdomain.repository.XpEventRepository;
import com.successhub.appdomain.service.GamificationEngine;
import com.successhub.appdomain.service.UserProfileService;
import com.successhub.appdomain.service.gamification.GamificationTimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplCompleteTest {

    @Mock TaskRepository taskRepository;
    @Mock XpEventRepository xpEventRepository;
    @Mock TaskCategoryRepository categoryRepository;
    @Mock GoalRepository goalRepository;
    @Mock GoalCategorySubscriptionRepository subscriptionRepository;
    @Mock UserProfileService profileService;
    @Mock GamificationEngine gamificationEngine;
    @Mock GamificationTimeUtil timeUtil;
    @Mock TaskMapper taskMapper;

    @InjectMocks TaskServiceImpl taskService;

    private UUID taskId;
    private Task task;
    private UserProfile profile;
    private ProfileDto profileDto;
    private TaskDto.Response taskDto;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        TaskCategory category = new TaskCategory();
        category.setGrantXp(true);

        task = new Task();
        task.setId(taskId);
        task.setUserId("user-1");
        task.setStatus(TaskStatus.TODO);
        task.setCategory(category);

        profile = new UserProfile();
        profileDto = new ProfileDto("user-1", "Test", 1, 100, 200, 0, 0, true, "en-US");
        taskDto = new TaskDto.Response(
                taskId, null, null, null, null, false, 0, "Title", "Desc", 0, 3, 30, 2,
                false, null, "TODO", null, null, null, null, null, null);

        when(taskRepository.findByIdAndUserId(taskId, "user-1")).thenReturn(Optional.of(task));
        when(taskMapper.toDto(any(Task.class))).thenReturn(taskDto);
        when(profileService.requireProfile("user-1")).thenReturn(profile);
        when(profileService.mapToDto(profile)).thenReturn(profileDto);
    }

    @Test
    void complete_doesNotRewardWhenXpAlreadyAwardedFlagSet() {
        // Arrange
        task.setXpAwarded(true);

        // Act
        TaskCompletionDto result = taskService.complete("user-1", taskId);

        // Assert
        verify(gamificationEngine, never()).processTaskCompletion(any(), any());
        assertNull(result.reward());
    }

    @Test
    void complete_doesNotRewardWhenXpEventAlreadyExists() {
        // Arrange
        when(xpEventRepository.existsByUserIdAndTaskId("user-1", taskId)).thenReturn(true);

        // Act
        TaskCompletionDto result = taskService.complete("user-1", taskId);

        // Assert
        verify(gamificationEngine, never()).processTaskCompletion(any(), any());
        assertNull(result.reward());
    }

    @Test
    void complete_rewardsFirstTimeOnly() {
        // Arrange
        when(xpEventRepository.existsByUserIdAndTaskId("user-1", taskId)).thenReturn(false);
        when(gamificationEngine.processTaskCompletion(eq("user-1"), eq(task)))
                .thenReturn(org.mockito.Mockito.mock(RewardEventDto.class));

        // Act
        taskService.complete("user-1", taskId);

        // Assert
        verify(gamificationEngine).processTaskCompletion("user-1", task);
    }
}
