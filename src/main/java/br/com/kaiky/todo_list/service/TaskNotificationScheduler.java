package br.com.kaiky.todo_list.service;

import java.time.LocalDateTime;

import br.com.kaiky.todo_list.dto.TaskNotificationMessage;
import br.com.kaiky.todo_list.dto.TaskResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskNotificationScheduler {

    private final TaskService taskService;
    private final TaskNotificationPublisher taskNotificationPublisher;
    private final int batchSize;
    private final int maxAttempts;

    public TaskNotificationScheduler(
            TaskService taskService,
            TaskNotificationPublisher taskNotificationPublisher,
            @Value("${app.notifications.batch-size}") int batchSize,
            @Value("${app.notifications.max-attempts}") int maxAttempts
    ) {
        this.taskService = taskService;
        this.taskNotificationPublisher = taskNotificationPublisher;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${app.notifications.scheduler-fixed-delay-ms}")
    public void queueDueTaskNotifications() {
        taskService.findDueNotifications(LocalDateTime.now(), batchSize, maxAttempts)
                .forEach(this::queueNotification);
    }

    private void queueNotification(TaskResponseDTO task) {
        try {
            TaskNotificationMessage message = taskService.markNotificationAsQueued(task.id());
            taskNotificationPublisher.publish(message);
        } catch (RuntimeException exception) {
            taskService.markNotificationAsFailed(task.id(), exception.getMessage());
        }
    }
}
