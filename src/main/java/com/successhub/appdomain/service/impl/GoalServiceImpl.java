package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.enums.TaskStatus;
import com.successhub.appdomain.enums.GoalStatus;
import com.successhub.appdomain.dto.GoalDto;
import com.successhub.appdomain.dto.GoalSubscriptionDto;
import com.successhub.appdomain.dto.TaskDto;
import com.successhub.appdomain.entity.Goal;
import com.successhub.appdomain.entity.GoalCategorySubscription;
import com.successhub.appdomain.entity.Task;
import com.successhub.appdomain.entity.TaskCategory;
import com.successhub.appdomain.repository.GoalCategorySubscriptionRepository;
import com.successhub.appdomain.repository.GoalRepository;
import com.successhub.appdomain.repository.TaskCategoryRepository;
import com.successhub.appdomain.repository.TaskRepository;
import com.successhub.appdomain.service.GoalService;
import com.successhub.appdomain.service.UserProfileService;
import com.successhub.coreinfra.exception.base.AppException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository categoryRepository;
    private final GoalCategorySubscriptionRepository subscriptionRepository;
    private final TaskMapper taskMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GoalDto.Response> getActiveGoals(String userId) {
        return repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalDto.Response> getCompletedGoals(String userId) {
        return repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.COMPLETED).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GoalDto.Response getById(String userId, UUID id) {
        return toDto(requireGoal(userId, id));
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
        Goal goal = requireGoal(userId, id);

        goal.setName(req.name());
        goal.setTier(req.tier());
        goal.setTargetDescription(req.targetDescription());
        goal.setTargetValue(req.targetValue());
        goal.setCurrentProgress(req.currentProgress());
        goal.setXpReward(req.xpReward());
        goal.setIcon(req.icon());
        goal.setFeatured(req.featured());

        if (req.status() != null) {
            GoalStatus newStatus = GoalStatus.valueOf(req.status());
            if (newStatus == GoalStatus.COMPLETED && goal.getStatus() != GoalStatus.COMPLETED) {
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
        Goal goal = requireGoal(userId, id);
        repository.delete(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto.Response> getLinkedTasks(String userId, UUID goalId) {
        requireGoal(userId, goalId);
        return taskRepository.findByUserIdAndGoal_IdOrderByCreatedAtDesc(userId, goalId).stream()
                .map(taskMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TaskDto.Response attachTask(String userId, UUID goalId, UUID taskId) {
        Goal goal = requireGoal(userId, goalId);
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
        task.setGoal(goal);
        return taskMapper.toDto(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskDto.Response detachTask(String userId, UUID goalId, UUID taskId) {
        requireGoal(userId, goalId);
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
        if (task.getGoal() == null || !task.getGoal().getId().equals(goalId)) {
            throw new IllegalArgumentException("Task is not linked to this campaign");
        }
        task.setGoal(null);
        return taskMapper.toDto(taskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalSubscriptionDto.Response> listSubscriptions(String userId, UUID goalId) {
        requireGoal(userId, goalId);
        return subscriptionRepository.findByGoal_IdAndUserId(goalId, userId).stream()
                .map(this::toSubscriptionDto)
                .toList();
    }

    @Override
    @Transactional
    public GoalSubscriptionDto.Response subscribeCategory(
            String userId, UUID goalId, GoalSubscriptionDto.SubscribeRequest req) {
        Goal goal = requireGoal(userId, goalId);
        TaskCategory category = categoryRepository.findById(req.categoryId())
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + req.categoryId()));

        subscriptionRepository.findByCategory_Id(category.getId()).ifPresent(existing -> {
            if (!existing.getGoal().getId().equals(goalId)) {
                throw new AppException(
                        "Category is already subscribed to another campaign",
                        HttpStatus.CONFLICT,
                        "CATEGORY_ALREADY_SUBSCRIBED");
            }
            throw new AppException(
                    "Category is already subscribed to this campaign",
                    HttpStatus.CONFLICT,
                    "CATEGORY_ALREADY_SUBSCRIBED");
        });

        GoalCategorySubscription sub = new GoalCategorySubscription();
        sub.setUserId(userId);
        sub.setGoal(goal);
        sub.setCategory(category);
        GoalCategorySubscription saved = subscriptionRepository.save(sub);

        List<Task> openUnlinked = taskRepository.findByUserIdAndCategory_IdAndGoalIsNullAndStatusNot(
                userId, category.getId(), TaskStatus.DONE);
        for (Task t : openUnlinked) {
            t.setGoal(goal);
        }
        if (!openUnlinked.isEmpty()) {
            taskRepository.saveAll(openUnlinked);
        }

        return toSubscriptionDto(saved);
    }

    @Override
    @Transactional
    public void unsubscribeCategory(String userId, UUID goalId, UUID categoryId) {
        requireGoal(userId, goalId);
        GoalCategorySubscription sub = subscriptionRepository
                .findByGoal_IdAndCategory_IdAndUserId(goalId, categoryId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        subscriptionRepository.delete(sub);
    }

    private Goal requireGoal(String userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + id));
    }

    private GoalSubscriptionDto.Response toSubscriptionDto(GoalCategorySubscription s) {
        return new GoalSubscriptionDto.Response(
                s.getId(),
                s.getGoal().getId(),
                s.getCategory().getId(),
                s.getCategory().getName(),
                s.getCreatedAt()
        );
    }

    private GoalDto.Response toDto(Goal g) {
        return new GoalDto.Response(
                g.getId(),
                g.getName(),
                g.getTier(),
                g.getTargetDescription(),
                g.getTargetValue(),
                g.getCurrentProgress(),
                g.getPendingDamage(),
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
