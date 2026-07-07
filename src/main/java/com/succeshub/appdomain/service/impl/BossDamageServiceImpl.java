package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.gamification.GamificationDto.BossDamageDto;
import com.succeshub.appdomain.model.Goal;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.appdomain.service.BossDamageService;
import com.succeshub.config.GamificationProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BossDamageServiceImpl implements BossDamageService {

    private final GoalRepository goalRepository;
    private final GamificationProperties properties;

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
        goal.setCurrentProgress(Math.min(goal.getTargetValue(), goal.getCurrentProgress() + delta));
        boolean completed = goal.getCurrentProgress() >= goal.getTargetValue();
        if (completed) {
            goal.setStatus(Goal.Status.COMPLETED);
            goal.setCompletedAt(Instant.now());
            goal.setSlainLabel("Slain");
        }
        goalRepository.save(goal);

        return new BossDamageDto(
                goal.getId(),
                goal.getName(),
                delta,
                goal.getHealthRemaining(),
                completed,
                completed ? goal.getXpReward() : 0
        );
    }
}
