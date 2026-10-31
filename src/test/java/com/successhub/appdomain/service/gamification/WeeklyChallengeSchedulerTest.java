package com.successhub.appdomain.service.gamification;

import com.successhub.appdomain.enums.TaskStatus;
import com.successhub.appdomain.entity.Task;
import com.successhub.appdomain.repository.TaskRepository;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.appdomain.service.LootBoxService;
import com.successhub.appdomain.config.GamificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyChallengeSchedulerTest {

    @Mock
    private UserProfileRepository profileRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private LootBoxService lootBoxService;

    private WeeklyChallengeScheduler scheduler;
    private GamificationTimeUtil timeUtil;

    @BeforeEach
    void setUp() {
        GamificationProperties properties = new GamificationProperties();
        properties.setWeeklyChallengeCount(2);
        properties.setWeeklyChallengeMinDifficulty(4);
        timeUtil = new GamificationTimeUtil();
        scheduler = new WeeklyChallengeScheduler(profileRepository, taskRepository, lootBoxService, properties, timeUtil);
    }

    @Test
    void assignForUser_flagsEligibleTasks() {
        // Arrange
        LocalDate monday = timeUtil.mondayOfWeek(timeUtil.today());
        Task task = new Task();
        task.setDifficulty(5);
        when(taskRepository.findStaleWeeklyChallenges(eq("user"), eq(monday))).thenReturn(List.of());
        when(taskRepository.findByUserIdAndStatusAndDifficultyGreaterThanEqualAndWeeklyChallengeFalseOrderByCreatedAtDesc(
                eq("user"), eq(TaskStatus.TODO), eq(4))).thenReturn(List.of(task, task, task));

        // Act
        scheduler.assignForUser("user", monday);

        // Assert
        verify(taskRepository, atLeastOnce()).save(any(Task.class));
    }
}
