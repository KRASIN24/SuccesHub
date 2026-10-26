package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.TaskDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.RewardEventDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.TaskCompletionDto;
import com.succeshub.appdomain.model.Goal;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.model.TaskCategory;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.appdomain.repository.TaskCategoryRepository;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.XpEventRepository;
import com.succeshub.appdomain.service.GamificationEngine;
import com.succeshub.appdomain.service.TaskService;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final XpEventRepository xpEventRepository;
    private final TaskCategoryRepository categoryRepository;
    private final GoalRepository goalRepository;
    private final UserProfileService profileService;
    private final GamificationEngine gamificationEngine;
    private final GamificationTimeUtil timeUtil;
    private final TaskMapper taskMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto.Response> getTasks(String userId) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(taskMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto.Response> getTasksByStatus(String userId, Task.Status status) {
        return taskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status).stream()
                .map(taskMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TaskDto.Response create(String userId, TaskDto.CreateRequest req) {
        Task task = new Task();
        task.setUserId(userId);
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setXpReward(req.xpReward());
        task.setMetaLabel(req.metaLabel());
        task.setMetaType(req.metaType());
        task.setDueDate(req.dueDate());
        task.setDifficulty(defaultInt(req.difficulty(), 3));
        task.setDurationMinutes(defaultInt(req.durationMinutes(), 30));
        task.setPriority(defaultInt(req.priority(), 2));
        task.setStarred(Boolean.TRUE.equals(req.starred()));
        applyReminder(task, req.reminderAt(), false);

        if (req.categoryId() != null) {
            TaskCategory cat = requireCategory(userId, req.categoryId());
            task.setCategory(cat);
        }

        applyParent(userId, task, req.parentTaskId());
        applyGoal(userId, task, req.goalId());

        if (req.sortOrder() != null) {
            task.setSortOrder(req.sortOrder());
        } else {
            task.setSortOrder(nextSortOrder(userId, task));
        }

        return taskMapper.toDto(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskDto.Response update(String userId, UUID id, TaskDto.UpdateRequest req) {
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));

        UUID previousCategoryId = task.getCategory() != null ? task.getCategory().getId() : null;

        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setXpReward(req.xpReward());
        task.setMetaLabel(req.metaLabel());
        task.setMetaType(req.metaType());
        task.setDueDate(req.dueDate());
        if (req.difficulty() != null) {
            task.setDifficulty(req.difficulty());
        }
        if (req.durationMinutes() != null) {
            task.setDurationMinutes(req.durationMinutes());
        }
        if (req.priority() != null) {
            task.setPriority(req.priority());
        }
        if (req.starred() != null) {
            task.setStarred(req.starred());
        }
        if (req.sortOrder() != null) {
            task.setSortOrder(req.sortOrder());
        }
        applyReminder(task, req.reminderAt(), Boolean.TRUE.equals(req.clearReminder()));

        if (req.status() != null) {
            Task.Status newStatus = Task.Status.valueOf(req.status());
            task.setStatus(newStatus);
            if (newStatus == Task.Status.DONE && task.getCompletedAt() == null) {
                task.setCompletedAt(Instant.now());
            } else if (newStatus != Task.Status.DONE) {
                task.setCompletedAt(null);
            }
        }

        if (Boolean.TRUE.equals(req.clearParent())) {
            task.setParentTask(null);
        } else if (req.parentTaskId() != null) {
            if (req.parentTaskId().equals(task.getId())) {
                throw new IllegalArgumentException("Task cannot be its own parent");
            }
            applyParent(userId, task, req.parentTaskId());
        }

        if (req.categoryId() != null) {
            TaskCategory cat = requireCategory(userId, req.categoryId());
            task.setCategory(cat);
            // Parent wins category when nesting; re-align after explicit category set
            if (task.getParentTask() != null) {
                Task parent = task.getParentTask();
                UUID parentCat = parent.getCategory() != null ? parent.getCategory().getId() : null;
                if (!Objects.equals(parentCat, cat.getId())) {
                    throw new IllegalArgumentException("Subtask category must match parent category");
                }
            }
        }

        applyGoal(userId, task, req.goalId());

        Task saved = taskRepository.save(task);
        cascadeCategoryToChildren(saved, previousCategoryId);
        return taskMapper.toDto(saved);
    }

    private void applyReminder(Task task, Instant reminderAt, boolean clear) {
        if (clear) {
            task.setReminderAt(null);
            task.setReminderNotified(false);
            return;
        }
        if (reminderAt == null) {
            return;
        }
        if (!Objects.equals(task.getReminderAt(), reminderAt)) {
            task.setReminderAt(reminderAt);
            task.setReminderNotified(false);
        }
    }

    private void cascadeCategoryToChildren(Task task, UUID previousCategoryId) {
        if (task.getParentTask() != null) {
            return;
        }
        UUID newCategoryId = task.getCategory() != null ? task.getCategory().getId() : null;
        if (Objects.equals(previousCategoryId, newCategoryId)) {
            return;
        }
        for (Task child : taskRepository.findByParentTask_Id(task.getId())) {
            child.setCategory(task.getCategory());
            taskRepository.save(child);
        }
    }

    @Override
    @Transactional
    public TaskCompletionDto complete(String userId, UUID id) {
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));

        if (task.getStatus() == Task.Status.DONE) {
            return new TaskCompletionDto(
                    taskMapper.toDto(task),
                    null,
                    profileService.mapToDto(profileService.requireProfile(userId)));
        }

        boolean alreadyRewarded = task.isXpAwarded()
                || xpEventRepository.existsByUserIdAndTaskId(userId, id);

        if (alreadyRewarded && !task.isXpAwarded()) {
            task.setXpAwarded(true);
        }

        task.setStatus(Task.Status.DONE);
        task.setCompletedAt(Instant.now());
        taskRepository.save(task);

        RewardEventDto reward = null;
        boolean grantXp = task.getCategory() == null || task.getCategory().isGrantXp();
        if (grantXp && !alreadyRewarded) {
            reward = gamificationEngine.processTaskCompletion(userId, task);
            task.setXpAwarded(true);
            taskRepository.save(task);
        }

        return new TaskCompletionDto(
                taskMapper.toDto(task),
                reward,
                profileService.mapToDto(profileService.requireProfile(userId)));
    }

    @Override
    @Transactional
    public void scheduleTasks(String userId, List<UUID> taskIds) {
        var today = timeUtil.today();
        for (UUID taskId : taskIds) {
            Task task = taskRepository.findByIdAndUserId(taskId, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
            task.setScheduledDate(today);
            taskRepository.save(task);
        }
    }

    @Override
    @Transactional
    public void delete(String userId, UUID id) {
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));
        taskRepository.delete(task);
    }

    @Override
    @Transactional
    public TaskDto.Response setStarred(String userId, UUID id, boolean starred) {
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));
        task.setStarred(starred);
        return taskMapper.toDto(taskRepository.save(task));
    }

    @Override
    @Transactional
    public void reorder(String userId, TaskDto.ReorderRequest req) {
        UUID categoryId = req.categoryId();
        UUID parentTaskId = req.parentTaskId();
        List<UUID> orderedIds = req.orderedIds();

        if (parentTaskId != null) {
            Task parent = taskRepository.findByIdAndUserId(parentTaskId, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Parent task not found: " + parentTaskId));
            if (parent.getParentTask() != null) {
                throw new IllegalArgumentException("Cannot reorder under a subtask (one level only)");
            }
            UUID parentCategoryId = parent.getCategory() != null ? parent.getCategory().getId() : null;
            if (!Objects.equals(parentCategoryId, categoryId)) {
                throw new IllegalArgumentException("parentTaskId category does not match categoryId");
            }
        } else if (categoryId != null) {
            requireCategory(userId, categoryId);
        }

        Map<UUID, Task> byId = orderedIds.stream()
                .map(id -> taskRepository.findByIdAndUserId(id, userId)
                        .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id)))
                .collect(Collectors.toMap(Task::getId, Function.identity(), (a, b) -> a));

        if (byId.size() != orderedIds.size()) {
            throw new IllegalArgumentException("orderedIds contains duplicates");
        }

        Set<UUID> expected = new HashSet<>();
        List<Task> siblings = parentTaskId != null
                ? taskRepository.findByUserIdAndCategory_IdAndParentTask_IdOrderBySortOrderAsc(
                        userId, categoryId, parentTaskId)
                : taskRepository.findByUserIdAndCategory_IdAndParentTaskIsNullOrderBySortOrderAsc(
                        userId, categoryId);
        for (Task s : siblings) {
            expected.add(s.getId());
        }
        if (!expected.equals(byId.keySet())) {
            throw new IllegalArgumentException("orderedIds must list exactly the sibling group");
        }

        for (int i = 0; i < orderedIds.size(); i++) {
            Task t = byId.get(orderedIds.get(i));
            t.setSortOrder(i);
            taskRepository.save(t);
        }
    }

    private void applyParent(String userId, Task task, UUID parentTaskId) {
        if (parentTaskId == null) {
            task.setParentTask(null);
            return;
        }
        if (task.getId() != null && taskRepository.countByParentTask_Id(task.getId()) > 0) {
            throw new IllegalArgumentException("Task with subtasks cannot become a subtask");
        }
        Task parent = taskRepository.findByIdAndUserId(parentTaskId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Parent task not found: " + parentTaskId));
        if (parent.getParentTask() != null) {
            throw new IllegalArgumentException("Subtasks cannot have children (one level only)");
        }
        UUID parentCategoryId = parent.getCategory() != null ? parent.getCategory().getId() : null;
        UUID taskCategoryId = task.getCategory() != null ? task.getCategory().getId() : null;
        if (!Objects.equals(parentCategoryId, taskCategoryId)) {
            // Align child category to parent when creating under a parent
            task.setCategory(parent.getCategory());
        }
        task.setParentTask(parent);
    }

    private int nextSortOrder(String userId, Task task) {
        if (task.getParentTask() != null) {
            return (int) taskRepository.countByParentTask_Id(task.getParentTask().getId());
        }
        UUID categoryId = task.getCategory() != null ? task.getCategory().getId() : null;
        if (categoryId == null) {
            return 0;
        }
        return (int) taskRepository.countByUserIdAndCategory_IdAndParentTaskIsNull(userId, categoryId);
    }

    private TaskCategory requireCategory(String userId, UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));
    }

    private void applyGoal(String userId, Task task, UUID goalId) {
        if (goalId == null) {
            task.setGoal(null);
            return;
        }
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + goalId));
        task.setGoal(goal);
    }

    private static int defaultInt(Integer value, int fallback) {
        return value != null ? value : fallback;
    }
}
