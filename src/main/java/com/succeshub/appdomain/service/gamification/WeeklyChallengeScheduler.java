package com.succeshub.appdomain.service.gamification;

import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.config.GamificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Assigns weekly challenge flags each Monday at 6 AM server time.
 * V1 uses {@link java.time.ZoneId#systemDefault()}; per-user timezone is deferred to V2.
 */
@Component
@RequiredArgsConstructor
public class WeeklyChallengeScheduler {

    private final UserProfileRepository profileRepository;
    private final TaskRepository taskRepository;
    private final GamificationProperties properties;
    private final GamificationTimeUtil timeUtil;

    @Scheduled(cron = "0 0 6 * * MON")
    @Transactional
    public void assignWeeklyChallenges() {
        LocalDate monday = timeUtil.mondayOfWeek(timeUtil.today());
        profileRepository.findAll().forEach(profile -> assignForUser(profile.getKeycloakId(), monday));
    }

    @Transactional
    public void assignForUser(String userId, LocalDate monday) {
        taskRepository.findStaleWeeklyChallenges(userId, monday).forEach(task -> {
            task.setWeeklyChallenge(false);
            task.setWeeklyChallengeWeek(null);
            taskRepository.save(task);
        });

        List<Task> eligible = taskRepository
                .findByUserIdAndStatusAndDifficultyGreaterThanEqualAndWeeklyChallengeFalseOrderByCreatedAtDesc(
                        userId, Task.Status.TODO, properties.getWeeklyChallengeMinDifficulty());

        int limit = Math.min(properties.getWeeklyChallengeCount(), eligible.size());
        for (int i = 0; i < limit; i++) {
            Task task = eligible.get(i);
            task.setWeeklyChallenge(true);
            task.setWeeklyChallengeWeek(monday);
            taskRepository.save(task);
        }
    }
}
