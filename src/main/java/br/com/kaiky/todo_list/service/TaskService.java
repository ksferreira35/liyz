package br.com.kaiky.todo_list.service;

import java.util.List;
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
                                  .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + id + " Not Found"));
        
        return convertToResponseDTO(task);
    }

    public List<TaskResponseDTO> findTasksByTitle(String title) {
        String normalizedTitle = normalizeTitle(title);
        UUID userId = authenticatedUserService.getCurrentUserId();

        return taskRepository
                .findByUser_IdAndTitleContainingIgnoreCase(userId, normalizedTitle)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    public TaskResponseDTO updateTaskStatus(UUID id, TaskStatusDTO dto) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                                  .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + id + " Not Found"));
        
        task.setCompleted(dto.completed());
        
        Task savedTask = taskRepository.save(task);

        return convertToResponseDTO(savedTask);
    }

    public TaskResponseDTO updateTaskStatusByTitle(String title, TaskStatusDTO dto) {
        Task task = findSingleTaskByTitleForCurrentUser(title);

        task.setCompleted(dto.completed());
        Task savedTask = taskRepository.save(task);

        return convertToResponseDTO(savedTask);
    }
    
    public void deleteTaskById(UUID id) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Task não encontrada"));

        taskRepository.delete(task);                        
    }

    public TaskResponseDTO deleteTaskByTitle(String title) {
        Task task = findSingleTaskByTitleForCurrentUser(title);
        TaskResponseDTO response = convertToResponseDTO(task);

        taskRepository.delete(task);

        return response;
    }

    public TaskResponseDTO convertToResponseDTO(Task task) {
        return new TaskResponseDTO(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.isCompleted()
        );

    }

    private Task findSingleTaskByTitleForCurrentUser(String title) {
        String normalizedTitle = normalizeTitle(title);
        UUID userId = authenticatedUserService.getCurrentUserId();
        List<Task> exactMatches = taskRepository.findByUser_IdAndTitleIgnoreCase(userId, normalizedTitle);

        if (exactMatches.size() == 1) {
            return exactMatches.getFirst();
        }

        if (exactMatches.size() > 1) {
            throw new IllegalArgumentException(
                    "More than one task was found with title: " + normalizedTitle + ". Ask the user to clarify."
            );
        }

        List<Task> partialMatches = taskRepository.findByUser_IdAndTitleContainingIgnoreCase(userId, normalizedTitle);

        if (partialMatches.size() == 1) {
            return partialMatches.getFirst();
        }

        if (partialMatches.isEmpty()) {
            throw new ResourceNotFoundException("Task with title: " + normalizedTitle + " not found");
        }

        throw new IllegalArgumentException(
                "More than one task matched: " + normalizedTitle + ". Ask the user to clarify."
        );
    }

    private String normalizeTitle(String title) {
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Task title is required");
        }

        return title.trim();
    }
}
