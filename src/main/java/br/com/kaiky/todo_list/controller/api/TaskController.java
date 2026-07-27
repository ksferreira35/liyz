package br.com.kaiky.todo_list.controller.api;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kaiky.todo_list.dto.TaskRequestDTO;
import br.com.kaiky.todo_list.dto.TaskResponseDTO;
import br.com.kaiky.todo_list.dto.TaskStatusDTO;
import br.com.kaiky.todo_list.dto.TaskUpdateDTO;
import br.com.kaiky.todo_list.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "List all tasks (Paginated)")
    @GetMapping
    public ResponseEntity<Page<TaskResponseDTO>> getAllTasks(
        @ParameterObject
        @PageableDefault(
            size = 10, 
            sort = "title",
            direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(taskService.getAllTasks(pageable));
    }

    @Operation(summary = "Get a task by ID")
    @ApiResponses
    ({
            @ApiResponse(responseCode = "200", description = "Task Found"),
            @ApiResponse(responseCode = "404", description = "Task Not Found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> findTaskById(
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(taskService.findTaskById(id));
    }
    
    @Operation(summary = "Create a task")
    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(
        @Valid @RequestBody TaskRequestDTO dto) {

        return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(taskService.createTask(dto));
    }

    @Operation(summary = "Update a task status")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateTaskStatus (
        @PathVariable UUID id,
        @RequestBody TaskStatusDTO statusDTO
    ) {
        taskService.updateTaskStatus(id, statusDTO);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a task")
    @PatchMapping("/{id}/details")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody TaskUpdateDTO dto
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, dto));
    }

    @Operation(summary = "Delete a task by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaskById(
        @PathVariable UUID id
    ) {

        taskService.deleteTaskById(id);

        return ResponseEntity.noContent().build();

    }

}
