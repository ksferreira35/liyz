package br.com.kaiky.todo_list.controller.view;

import br.com.kaiky.todo_list.dto.ScheduleSectionDTO;
import br.com.kaiky.todo_list.dto.TaskStatusDTO;
import br.com.kaiky.todo_list.service.TaskService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequestMapping("/schedule")
public class ScheduleViewController {

    private final TaskService taskService;

    public ScheduleViewController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public String schedulePage(
            @RequestParam(defaultValue = "today") String filter,
            Model model
    ) {
        addScheduleModelAttributes(model, filter);

        return "schedule";
    }

    @GetMapping("/fragments/list")
    public String scheduleListFragment(
            @RequestParam(defaultValue = "today") String filter,
            Model model
    ) {
        addScheduleModelAttributes(model, filter);

        return "schedule :: scheduleList";
    }

    @PatchMapping("/{id}/status")
    public String updateTaskStatus(
            @PathVariable UUID id,
            @ModelAttribute TaskStatusDTO statusDTO,
            @RequestParam(defaultValue = "today") String filter,
            Model model
    ) {
        taskService.updateTaskStatus(id, statusDTO);
        addScheduleModelAttributes(model, filter);

        return "schedule :: scheduleList";
    }

    @PatchMapping("/{id}/disable-reminder")
    public String disableReminder(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "today") String filter,
            Model model
    ) {
        taskService.disableNotification(id);
        addScheduleModelAttributes(model, filter);

        return "schedule :: scheduleList";
    }

    private void addScheduleModelAttributes(Model model, String filter) {
        String normalizedFilter = normalizeFilter(filter);
        List<ScheduleSectionDTO> sections = taskService.getSchedule(normalizedFilter);

        model.addAttribute("filter", normalizedFilter);
        model.addAttribute("filterLabel", filterLabel(normalizedFilter));
        model.addAttribute("sections", sections);
        model.addAttribute(
                "totalTasks",
                sections
                        .stream()
                        .mapToInt(section -> section.tasks().size())
                        .sum()
        );
    }

    private String normalizeFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return "today";
        }

        return switch (filter.trim().toLowerCase(Locale.ENGLISH)) {
            case "today", "tomorrow", "week", "later", "no-date" -> filter.trim().toLowerCase(Locale.ENGLISH);
            default -> "today";
        };
    }

    private String filterLabel(String filter) {
        return switch (filter) {
            case "tomorrow" -> "Tomorrow";
            case "week" -> "Next 7 days";
            case "later" -> "Later";
            case "no-date" -> "No date";
            default -> "Today";
        };
    }
}
