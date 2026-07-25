package br.com.kaiky.todo_list.controller.view;

import br.com.kaiky.todo_list.dto.TaskRequestDTO;
import br.com.kaiky.todo_list.dto.TaskResponseDTO;
import br.com.kaiky.todo_list.dto.TaskStatusDTO;
import br.com.kaiky.todo_list.service.TaskService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/tasks")
public class TaskViewController {

    private final TaskService taskService;

    public TaskViewController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public String tasksPage(
            Pageable pageable,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Model model
    ) {
        addTaskListModelAttributes(model, pageable, sortDirection);

        model.addAttribute(
                "taskRequest",
                new TaskRequestDTO("","")
        );

        return "tasks";
    }

    @GetMapping("/fragments/list")
    public String taskListFragment(
            Pageable pageable,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Model model
    ) {
        addTaskListModelAttributes(model, pageable, sortDirection);

        return "tasks :: taskListSection";
    }

    @GetMapping("/{id}")
    public String getTask(
            @PathVariable UUID id,
            Model model
    ) {
        model.addAttribute(
                "task",
                taskService.findTaskById(id)
        );

        return "task";
    }

    @PostMapping
    public String createTask(
            @Valid
            @ModelAttribute("taskRequest")
            TaskRequestDTO dto,

            BindingResult bindingResult,
            Model model,
            HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors());
            response.setHeader("HX-Retarget", "#task-form-errors");
            response.setHeader("HX-Reswap", "innerHTML");
            return "fragments/task-form-errors :: formErrors";
        }

        TaskResponseDTO createdTask = taskService.createTask(dto);

        model.addAttribute("task", createdTask);

        return "fragments/task-item :: taskItem(task=${task})";
    }

    @PatchMapping("/{id}")
    public String updateTaskStatus(
            @PathVariable UUID id,
            @ModelAttribute TaskStatusDTO statusDTO,
            Model model
    ) {
        taskService.updateTaskStatus(id, statusDTO);

        model.addAttribute(
                "task",
                taskService.findTaskById(id)
        );

        return "fragments/task-item :: taskItem(task=${task})";
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID id
    ) {
        taskService.deleteTaskById(id);
        return ResponseEntity.ok().build();
    }

    private void addTaskListModelAttributes(
            Model model,
            Pageable pageable,
            String sortDirection
    ) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(direction, "createdAt")
        );

        model.addAttribute("tasks", taskService.getAllTasks(sortedPageable));
        model.addAttribute("sortDirection", direction.name().toLowerCase());
        model.addAttribute("nextSortDirection", direction.isAscending() ? "desc" : "asc");
    }
}
