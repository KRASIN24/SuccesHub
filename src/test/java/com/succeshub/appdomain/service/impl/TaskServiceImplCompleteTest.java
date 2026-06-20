package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.ProfileDto;
import com.succeshub.appdomain.dto.TaskDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.RewardEventDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.TaskCompletionDto;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.model.TaskCategory;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.appdomain.repository.TaskCategoryRepository;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.XpEventRepository;
import com.succeshub.appdomain.service.GamificationEngine;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
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
        task.setStatus(Task.Status.TODO);
        task.setCategory(category);

        profile = new UserProfile();
        profileDto = new ProfileDto("user-1", "Test", 1, 100, 200, 0, 0);
        taskDto = new TaskDto.Response(
                taskId, null, null, null, "Title", "Desc", 0, 3, 30, 2,
                false, null, "TODO", null, null, null, null, null);

        when(taskRepository.findByIdAndUserId(taskId, "user-1")).thenReturn(Optional.of(task));
        when(taskMapper.toDto(any(Task.class))).thenReturn(taskDto);
        when(profileService.requireProfile("user-1")).thenReturn(profile);
        when(profileService.mapToDto(profile)).thenReturn(profileDto);
    }

    @Test
    void complete_doesNotRewardWhenXpAlreadyAwardedFlagSet() {
        task.setXpAwarded(true);

        TaskCompletionDto result = taskService.complete("user-1", taskId);

        verify(gamificationEngine, never()).processTaskCompletion(any(), any());
        assertNull(result.reward());
    }

    @Test
    void complete_doesNotRewardWhenXpEventAlreadyExists() {
        when(xpEventRepository.existsByUserIdAndTaskId("user-1", taskId)).thenReturn(true);

        TaskCompletionDto result = taskService.complete("user-1", taskId);

        verify(gamificationEngine, never()).processTaskCompletion(any(), any());
        assertNull(result.reward());
    }

    @Test
    void complete_rewardsFirstTimeOnly() {
        when(xpEventRepository.existsByUserIdAndTaskId("user-1", taskId)).thenReturn(false);
        when(gamificationEngine.processTaskCompletion(eq("user-1"), eq(task)))
                .thenReturn(org.mockito.Mockito.mock(RewardEventDto.class));

        taskService.complete("user-1", taskId);

        verify(gamificationEngine).processTaskCompletion("user-1", task);
    }
}
