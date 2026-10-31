package com.successhub.appdomain.service;

import com.successhub.appdomain.event.GameNotificationEvent;
import com.successhub.appdomain.enums.NotificationType;
import com.successhub.appdomain.entity.Task;
import com.successhub.appdomain.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Fires in-app {@link NotificationType#TASK_REMINDER} notifications when task reminders become due.
 */
@Component
@RequiredArgsConstructor
public class TaskReminderScheduler {

    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher events;

    @Scheduled(fixedDelayString = "${successhub.tasks.reminder-poll-ms:60000}")
    @Transactional
    public void fireDueReminders() {
        Instant now = Instant.now();
        List<Task> due = taskRepository.findDueReminders(now);
        for (Task task : due) {
            String title = task.getTitle();
            if (title.length() > 80) {
                title = title.substring(0, 77) + "...";
            }
            events.publishEvent(new GameNotificationEvent(
                    task.getUserId(),
                    NotificationType.TASK_REMINDER,
                    title,
                    "Reminder: this task is due for your attention.",
                    "/tasks?focus=" + task.getId(),
                    task.getId()));
            task.setReminderNotified(true);
            taskRepository.save(task);
        }
    }
}
