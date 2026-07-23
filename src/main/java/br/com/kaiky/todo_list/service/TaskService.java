package br.com.kaiky.todo_list.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.kaiky.todo_list.dto.TaskRequestDTO;
import br.com.kaiky.todo_list.dto.TaskResponseDTO;
import br.com.kaiky.todo_list.dto.TaskStatusDTO;
import br.com.kaiky.todo_list.entity.AppUser;
import br.com.kaiky.todo_list.entity.Task;
import br.com.kaiky.todo_list.exception.ResourceNotFoundException;
import br.com.kaiky.todo_list.repository.TaskRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public TaskService(
            TaskRepository taskRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.taskRepository = taskRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    public TaskResponseDTO createTask(TaskRequestDTO dto) {
        AppUser user = authenticatedUserService.getCurrentUser();
        
        Task task = new Task(
            dto.title(),
            dto.description()
        );
        task.setUser(user);

        Task saveTask = taskRepository.save(task);

        return convertToResponseDTO(saveTask);
    }

    public Page<TaskResponseDTO> getAllTasks(Pageable pageable) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Page<Task> tasks = taskRepository.findAllByUserId(userId, pageable);

        return tasks.map(this::convertToResponseDTO);
    }

    public TaskResponseDTO findTaskById(UUID id) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                                  .orElseThrow(() -> new ResourceNotFoundException("Task with id: "+ id + "Not Found"));
        
        return convertToResponseDTO(task);
    }

    public void updateTaskStatus(UUID id, TaskStatusDTO dto) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                                  .orElseThrow(() -> new ResourceNotFoundException("Task with id: "+ id + "Not Found"));
        
        task.setCompleted(dto.completed());
        
        taskRepository.save(task);
    }
    
    public void deleteTaskById(UUID id) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Task não encontrada"));

        taskRepository.delete(task);                        
    }

    public TaskResponseDTO convertToResponseDTO(Task task) {
        return new TaskResponseDTO(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.isCompleted()
        );

    }
}
