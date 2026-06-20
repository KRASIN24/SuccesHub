package com.succeshub.appdomain.service.gamification;

import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.config.GamificationProperties;
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

    private WeeklyChallengeScheduler scheduler;
    private GamificationTimeUtil timeUtil;

    @BeforeEach
    void setUp() {
        GamificationProperties properties = new GamificationProperties();
        properties.setWeeklyChallengeCount(2);
        properties.setWeeklyChallengeMinDifficulty(4);
        timeUtil = new GamificationTimeUtil();
        scheduler = new WeeklyChallengeScheduler(profileRepository, taskRepository, properties, timeUtil);
    }

    @Test
    void assignForUser_flagsEligibleTasks() {
        LocalDate monday = timeUtil.mondayOfWeek(timeUtil.today());
        Task task = new Task();
        task.setDifficulty(5);
        when(taskRepository.findStaleWeeklyChallenges(eq("user"), eq(monday))).thenReturn(List.of());
        when(taskRepository.findByUserIdAndStatusAndDifficultyGreaterThanEqualAndWeeklyChallengeFalseOrderByCreatedAtDesc(
                eq("user"), eq(Task.Status.TODO), eq(4))).thenReturn(List.of(task, task, task));

        scheduler.assignForUser("user", monday);

        verify(taskRepository, atLeastOnce()).save(any(Task.class));
    }
}
