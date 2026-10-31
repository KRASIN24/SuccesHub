package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.dto.TaskDto;
import com.successhub.appdomain.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskDto.Response toDto(Task t) {
        return new TaskDto.Response(
                t.getId(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getGoal() != null ? t.getGoal().getId() : null,
                t.getParentTask() != null ? t.getParentTask().getId() : null,
                t.isStarred(),
                t.getSortOrder(),
                t.getTitle(),
                t.getDescription(),
                t.getXpReward(),
                t.getDifficulty(),
                t.getDurationMinutes(),
                t.getPriority(),
                t.isWeeklyChallenge(),
                t.getScheduledDate(),
                t.getStatus().name(),
                t.getMetaLabel(),
                t.getMetaType(),
                t.getDueDate(),
                t.getReminderAt(),
                t.getCompletedAt(),
                t.getCreatedAt()
        );
    }
}
