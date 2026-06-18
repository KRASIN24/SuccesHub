package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.GoalDto;
import com.succeshub.appdomain.model.Goal;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.appdomain.service.GoalService;
import com.succeshub.appdomain.service.UserProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final GoalRepository repository;
    private final UserProfileService profileService;

    @Override
    @Transactional(readOnly = true)
    public List<GoalDto.Response> getActiveGoals(String userId) {
        return repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Goal.Status.ACTIVE).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalDto.Response> getCompletedGoals(String userId) {
        return repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Goal.Status.COMPLETED).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GoalDto.SummaryResponse getSummary(String userId) {
        long total = repository.countTotal(userId);
        long completed = repository.countCompleted(userId);
        int percent = total == 0 ? 0 : (int) Math.round(((double) completed / total) * 100);
        return new GoalDto.SummaryResponse(total, completed, percent);
    }

    @Override
    @Transactional
    public GoalDto.Response create(String userId, GoalDto.CreateRequest req) {
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setName(req.name());
        goal.setTier(req.tier());
        goal.setTargetDescription(req.targetDescription());
        goal.setTargetValue(req.targetValue());
        goal.setCurrentProgress(req.currentProgress());
        goal.setXpReward(req.xpReward());
        goal.setIcon(req.icon());
        goal.setFeatured(req.featured());
        return toDto(repository.save(goal));
    }

    @Override
    @Transactional
    public GoalDto.Response update(String userId, UUID id, GoalDto.UpdateRequest req) {
        Goal goal = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + id));

        goal.setName(req.name());
        goal.setTier(req.tier());
        goal.setTargetDescription(req.targetDescription());
        goal.setTargetValue(req.targetValue());
        goal.setCurrentProgress(req.currentProgress());
        goal.setXpReward(req.xpReward());
        goal.setIcon(req.icon());
        goal.setFeatured(req.featured());

        if (req.status() != null) {
            Goal.Status newStatus = Goal.Status.valueOf(req.status());
            if (newStatus == Goal.Status.COMPLETED && goal.getStatus() != Goal.Status.COMPLETED) {
                goal.setCompletedAt(Instant.now());
                if (goal.getXpReward() > 0) {
                    profileService.addXp(userId, goal.getXpReward());
                }
            }
            goal.setStatus(newStatus);
        }

        return toDto(repository.save(goal));
    }

    @Override
    @Transactional
    public void delete(String userId, UUID id) {
        Goal goal = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + id));
        repository.delete(goal);
    }

    private GoalDto.Response toDto(Goal g) {
        return new GoalDto.Response(
                g.getId(),
                g.getName(),
                g.getTier(),
                g.getTargetDescription(),
                g.getTargetValue(),
                g.getCurrentProgress(),
                g.getHealthRemaining(),
                g.getXpReward(),
                g.getIcon(),
                g.getStatus().name(),
                g.isFeatured(),
                g.getSlainLabel(),
                g.getCompletedAt(),
                g.getCreatedAt()
        );
    }
}
