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

import java.util.List;

import java.util.UUID;



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



        if (req.categoryId() != null) {

            TaskCategory cat = categoryRepository.findById(req.categoryId())

                    .filter(c -> c.getUserId().equals(userId))

                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + req.categoryId()));

            task.setCategory(cat);

        }



        applyGoal(userId, task, req.goalId());



        return taskMapper.toDto(taskRepository.save(task));

    }



    @Override

    @Transactional

    public TaskDto.Response update(String userId, UUID id, TaskDto.UpdateRequest req) {

        Task task = taskRepository.findByIdAndUserId(id, userId)

                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));



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



        if (req.status() != null) {
            Task.Status newStatus = Task.Status.valueOf(req.status());
            task.setStatus(newStatus);
            if (newStatus == Task.Status.DONE && task.getCompletedAt() == null) {
                task.setCompletedAt(Instant.now());
            } else if (newStatus != Task.Status.DONE) {
                task.setCompletedAt(null);
            }
        }



        if (req.categoryId() != null) {

            TaskCategory cat = categoryRepository.findById(req.categoryId())

                    .filter(c -> c.getUserId().equals(userId))

                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + req.categoryId()));

            task.setCategory(cat);

        }



        applyGoal(userId, task, req.goalId());



        return taskMapper.toDto(taskRepository.save(task));

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


