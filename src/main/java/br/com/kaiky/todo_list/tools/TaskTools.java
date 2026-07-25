package br.com.kaiky.todo_list.tools;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import br.com.kaiky.todo_list.dto.TaskRequestDTO;
import br.com.kaiky.todo_list.dto.TaskResponseDTO;
import br.com.kaiky.todo_list.dto.TaskStatusDTO;
import br.com.kaiky.todo_list.service.TaskService;

@Component
public class TaskTools {
    private final TaskService taskService;

    public TaskTools(TaskService taskService) {
        this.taskService = taskService;
    }

    @Tool(
            description = """
                Retrieves a task using its UUID.
                Use this tool when the user asks for information
                about a specific task and provides its ID.
            """
    )
    public TaskResponseDTO findTaskById(
            @ToolParam(description = "Task UUID provided by the user.")
            UUID id
    ){
        return taskService.findTaskById(id);
    }

    @Tool(
            description = """
                Finds tasks by title or a partial title.
                Use this when the user refers to a task by name instead of UUID,
                for example "Assistir TV", "review dashboard", or "minha task de almoço".
                If more than one task is returned, ask the user to clarify which one.
            """
    )
    public List<TaskResponseDTO> findTasksByTitle(
            @ToolParam(description = "Task title or partial title mentioned by the user.")
            String title
    ){
        return taskService.findTasksByTitle(title);
    }

    @Tool(
            description = """
        Retrieves a paginated list of tasks.

        Use page 0 and size 20 when the user does not specify pagination.
        """
    )
    public List<TaskResponseDTO> getAllTasks(
            @ToolParam(description = "Zero-based page number. Use 0 if the user does not specify pagination.")
            int page,
            @ToolParam(description = "Page size. Use 20 if the user does not specify pagination.")
            int size
    ) {
        return taskService
                .getAllTasks(PageRequest.of(page, size))
                .getContent();
    }

    @Tool(
            description = """
                Creates a new task.
                Use this tool when the user explicitly asks to create a task.
                A title is required and a description is optional.
                For a single task creation request, call this tool exactly once.
                Do not call it again to refine, confirm, complete, or correct
                a task that was already created.
                If the user allows you to invent details, choose them before
                calling this tool and include them in the same description.
            """
    )
    public TaskResponseDTO createTask(
            @ToolParam(description = "Task title exactly as requested by the user. Maximum 120 characters.")
            String title,
            @ToolParam(description = "Task description. Use an empty string when the user does not provide one. Maximum 2000 characters.")
            String description
    ){
        return taskService.createTask(new TaskRequestDTO(title, description));
    }

    @Tool(
            description = """
                Creates a new task with an email reminder.
                Use this when the user asks to be reminded, notified, alerted,
                or gives a date/time for the task reminder.
                The reminder will be sent to the authenticated user's email.
                Never ask the user for their email address.
            """
    )
    public TaskResponseDTO createTaskWithReminder(
            @ToolParam(description = "Task title exactly as requested by the user. Maximum 120 characters.")
            String title,
            @ToolParam(description = "Task description. Use an empty string when the user does not provide one. Maximum 2000 characters.")
            String description,
            @ToolParam(description = "Reminder date and time in ISO local date-time format, for example 2026-07-25T20:30:00.")
            LocalDateTime dueAt
    ){
        return taskService.createTask(new TaskRequestDTO(title, description, dueAt, true));
    }

    @Tool(
            description = """
                Updates the completion status of existing task.
                Use completed=true to mark the task as completed.
                And completed=false to mark the task is pending.
            """
    )
    public TaskResponseDTO updateTaskStatus(
            @ToolParam(description = "Task UUID provided by the user.")
            UUID id,
            @ToolParam(description = "true marks as completed; false marks as pending.")
            boolean completed
    ){
        return taskService.updateTaskStatus(
                id,
                new TaskStatusDTO(completed));
    }

    @Tool(
            description = """
                Marks exactly one task as completed using its title or partial title.
                Use this when the user says the task is completed, done, ready, finished,
                marked, closed, pronta, concluída, finalizada, feita, or terminada,
                and identifies the task by name instead of UUID.
                If multiple tasks match, ask the user to clarify instead of guessing.
            """
    )
    public TaskResponseDTO markTaskAsCompletedByTitle(
            @ToolParam(description = "Task title or partial title mentioned by the user.")
            String title
    ){
        return taskService.updateTaskStatusByTitle(title, new TaskStatusDTO(true));
    }

    @Tool(
            description = """
                Marks exactly one task as pending using its title or partial title.
                Use this when the user says the task should be pending, not done,
                reopened, open again, pendente, não concluída, or reaberta,
                and identifies the task by name instead of UUID.
                If multiple tasks match, ask the user to clarify instead of guessing.
            """
    )
    public TaskResponseDTO markTaskAsPendingByTitle(
            @ToolParam(description = "Task title or partial title mentioned by the user.")
            String title
    ){
        return taskService.updateTaskStatusByTitle(title, new TaskStatusDTO(false));
    }

    @Tool(
            description = """
                Permanently deletes a task using its UUID.
                Only use this tool when the user clearly and explicitly
                requests deletion.
                Do not execute it for ambiguous requests.
            """
    )
    public TaskResponseDTO deleteTask(
            @ToolParam(description = "Task UUID provided by the user.")
            UUID id
    ){
        TaskResponseDTO response = taskService.findTaskById(id);
        taskService.deleteTaskById(id);

        return response;
    }

    @Tool(
            description = """
                Permanently deletes exactly one task using its title or partial title.
                Use this only when the user clearly asks to delete, remove, erase,
                excluir, deletar, apagar, or remover a task and identifies it by name
                instead of UUID.
                If multiple tasks match, ask the user to clarify instead of guessing.
            """
    )
    public TaskResponseDTO deleteTaskByTitle(
            @ToolParam(description = "Task title or partial title mentioned by the user.")
            String title
    ){
        return taskService.deleteTaskByTitle(title);
    }

}
