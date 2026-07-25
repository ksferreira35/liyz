package br.com.kaiky.todo_list.service;

import br.com.kaiky.todo_list.dto.TaskNotificationMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class TaskNotificationConsumer {

    private final TaskService taskService;
    private final TaskNotification taskNotification;

    public TaskNotificationConsumer(
            TaskService taskService,
            TaskNotification taskNotification
    ) {
        this.taskService = taskService;
        this.taskNotification = taskNotification;
    }

    @RabbitListener(queues = "${app.notifications.rabbit.queue}")
    public void consume(TaskNotificationMessage message) {
        try {
            TaskNotification.TaskNotificationEmail email = taskService
                    .buildNotificationEmail(message.taskId(), message.userId());

            taskNotification.sendTaskNotification(email);
            taskService.markNotificationAsSent(message.taskId(), message.userId());
        } catch (RuntimeException exception) {
            taskService.markNotificationAsFailed(message.taskId(), exception.getMessage());
        }
    }
}
