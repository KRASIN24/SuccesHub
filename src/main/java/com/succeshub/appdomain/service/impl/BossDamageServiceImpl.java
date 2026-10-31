package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.gamification.GamificationDto.BossDamageDto;
import com.succeshub.appdomain.event.GameNotificationEvent;
import com.succeshub.appdomain.model.Goal;
import com.succeshub.appdomain.model.NotificationType;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.appdomain.service.BossDamageService;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.config.GamificationProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Banks linked-task damage into {@link Goal#getPendingDamage()} and releases it on Strike.
 */
@Service
@RequiredArgsConstructor
public class BossDamageServiceImpl implements BossDamageService {

    private final GoalRepository goalRepository;
    private final GamificationProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final UserProfileService profileService;

    @Override
    @Transactional
    public BossDamageDto applyDamage(String userId, Task task) {
        if (task.getGoal() == null) {
            return null;
        }
        Goal goal = goalRepository.findByIdAndUserId(task.getGoal().getId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));
        if (goal.getStatus() != Goal.Status.ACTIVE) {
            return null;
        }

        double delta = task.getDifficulty() * properties.getBossDamageFactor();
        goal.setPendingDamage(goal.getPendingDamage() + delta);
        goalRepository.save(goal);

        return new BossDamageDto(
                goal.getId(),
                goal.getName(),
                delta,
                goal.getHealthRemaining(),
                false,
                0
        );
    }

    @Override
    @Transactional
    public BossDamageDto strike(String userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));
        if (goal.getStatus() != Goal.Status.ACTIVE) {
            throw new IllegalStateException("Campaign is not active");
        }
        double pending = goal.getPendingDamage();
        if (pending <= 0) {
            throw new IllegalStateException("No pending damage to release");
        }

        double applied = Math.min(pending, goal.getAbsoluteHealthRemaining());
        goal.setCurrentProgress(Math.min(goal.getTargetValue(), goal.getCurrentProgress() + applied));
        goal.setPendingDamage(0);
        boolean completed = goal.getCurrentProgress() >= goal.getTargetValue();
        if (completed) {
            goal.setStatus(Goal.Status.COMPLETED);
            goal.setCompletedAt(Instant.now());
            goal.setSlainLabel("Slain");
        }
        goalRepository.save(goal);

        int xp = 0;
        if (completed && goal.getXpReward() > 0) {
            xp = goal.getXpReward();
            profileService.addXp(userId, xp);
        }

        if (completed) {
            eventPublisher.publishEvent(new GameNotificationEvent(
                    userId,
                    NotificationType.BOSS_DEFEATED,
                    "Boss defeated",
                    goal.getName() + " has been slain.",
                    "/goals",
                    goal.getId()));
        }

        return new BossDamageDto(
                goal.getId(),
                goal.getName(),
                applied,
                goal.getHealthRemaining(),
                completed,
                xp
        );
    }
}
