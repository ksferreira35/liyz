package br.com.kaiky.todo_list.controller.view;

import java.util.List;
import java.util.UUID;

import br.com.kaiky.todo_list.entity.NotificationStatus;
import br.com.kaiky.todo_list.service.TaskService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/notifications")
public class NotificationViewController {

    private final TaskService taskService;

    public NotificationViewController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public String notificationsPage(
            @RequestParam(defaultValue = "upcoming") String filter,
            Model model
    ) {
        addNotificationsModel(model, filter);

        return "notifications";
    }

    @GetMapping("/fragments/list")
    public String notificationsListFragment(
            @RequestParam(defaultValue = "upcoming") String filter,
            Model model
    ) {
        addNotificationsModel(model, filter);

        return "notifications :: notificationsList";
    }

    @PatchMapping("/{id}/retry")
    public String retryNotification(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "failed") String filter,
            Model model
    ) {
        taskService.retryNotification(id);
        addNotificationsModel(model, filter);

        return "notifications :: notificationsList";
    }

    @PatchMapping("/{id}/disable")
    public String disableNotification(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "upcoming") String filter,
            Model model
    ) {
        taskService.disableNotification(id);
        addNotificationsModel(model, filter);

        return "notifications :: notificationsList";
    }

    private void addNotificationsModel(Model model, String filter) {
        List<NotificationStatus> statuses = statusesFor(filter);

        model.addAttribute("filter", filter);
        model.addAttribute("notifications", taskService.findNotificationTasks(statuses));
    }

    private List<NotificationStatus> statusesFor(String filter) {
        return switch (filter.toLowerCase()) {
            case "queued" -> List.of(NotificationStatus.QUEUED);
            case "sent" -> List.of(NotificationStatus.SENT);
            case "failed" -> List.of(NotificationStatus.FAILED);
            default -> List.of(NotificationStatus.PENDING);
        };
    }
}
