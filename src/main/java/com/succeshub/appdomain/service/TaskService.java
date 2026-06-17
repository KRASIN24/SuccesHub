package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.TaskDto;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.model.TaskCategory;
import com.succeshub.appdomain.repository.TaskCategoryRepository;
import com.succeshub.appdomain.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCategoryRepository categoryRepository;
    private final UserProfileService profileService;

    @Transactional(readOnly = true)
    public List<TaskDto.Response> getTasks(String userId) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDto.Response> getTasksByStatus(String userId, Task.Status status) {
        return taskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status).stream()
                .map(this::toDto)
                .toList();
    }

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

        if (req.categoryId() != null) {
            TaskCategory cat = categoryRepository.findById(req.categoryId())
                    .filter(c -> c.getUserId().equals(userId))
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + req.categoryId()));
            task.setCategory(cat);
        }

        return toDto(taskRepository.save(task));
    }

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

        if (req.status() != null) {
            task.setStatus(Task.Status.valueOf(req.status()));
        }

        if (req.categoryId() != null) {
            TaskCategory cat = categoryRepository.findById(req.categoryId())
                    .filter(c -> c.getUserId().equals(userId))
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + req.categoryId()));
            task.setCategory(cat);
        }

        return toDto(taskRepository.save(task));
    }

    /**
     * Marks the task DONE, awards XP if its category grants XP,
     * and delegates the XP update to UserProfileService.
     */
    @Transactional
    public TaskDto.Response complete(String userId, UUID id) {
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));

        if (task.getStatus() == Task.Status.DONE) {
            return toDto(task); // idempotent
        }

        task.setStatus(Task.Status.DONE);
        task.setCompletedAt(Instant.now());
        taskRepository.save(task);

        boolean grantXp = task.getCategory() == null || task.getCategory().isGrantXp();
        if (grantXp && task.getXpReward() > 0) {
            profileService.addXp(userId, task.getXpReward());
        }

        return toDto(task);
    }

    @Transactional
    public void delete(String userId, UUID id) {
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));
        taskRepository.delete(task);
    }

    private TaskDto.Response toDto(Task t) {
        return new TaskDto.Response(
                t.getId(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getTitle(),
                t.getDescription(),
                t.getXpReward(),
                t.getStatus().name(),
                t.getMetaLabel(),
                t.getMetaType(),
                t.getDueDate(),
                t.getCompletedAt(),
                t.getCreatedAt()
        );
    }
}
