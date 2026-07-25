package br.com.kaiky.todo_list.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kaiky.todo_list.dto.TaskNotificationMessage;
import br.com.kaiky.todo_list.dto.TaskRequestDTO;
import br.com.kaiky.todo_list.dto.TaskResponseDTO;
import br.com.kaiky.todo_list.dto.TaskStatusDTO;
import br.com.kaiky.todo_list.entity.AppUser;
import br.com.kaiky.todo_list.entity.NotificationStatus;
import br.com.kaiky.todo_list.entity.Task;
import br.com.kaiky.todo_list.exception.ResourceNotFoundException;
import br.com.kaiky.todo_list.repository.TaskRepository;

@Service
public class TaskService {

    private static final int TITLE_MAX_LENGTH = 120;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;

    private final TaskRepository taskRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public TaskService(
            TaskRepository taskRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.taskRepository = taskRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional
    public TaskResponseDTO createTask(TaskRequestDTO dto) {
        validateTaskRequest(dto);
        AppUser user = authenticatedUserService.getCurrentUser();
        
        Task task = new Task(
            dto.title(),
            dto.description()
        );
        task.setUser(user);
        task.setDueAt(dto.dueAt());
        task.setNotificationStatus(resolveInitialNotificationStatus(dto));

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
    
    @Transactional
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

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> findDueNotifications(LocalDateTime now, int batchSize, int maxAttempts) {
        return taskRepository
                .findDueNotifications(
                        now,
                        maxAttempts,
                        PageRequest.of(0, batchSize)
                )
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    @Transactional
    public TaskNotificationMessage markNotificationAsQueued(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + taskId + " not found"));

        if (task.getNotificationStatus() != NotificationStatus.PENDING
                && task.getNotificationStatus() != NotificationStatus.FAILED) {
            throw new IllegalStateException("Task notification is not ready to be queued");
        }

        task.setNotificationStatus(NotificationStatus.QUEUED);
        task.setNotificationQueuedAt(LocalDateTime.now());
        task.setLastNotificationError(null);
        Task savedTask = taskRepository.save(task);

        return new TaskNotificationMessage(savedTask.getId(), savedTask.getUser().getId());
    }

    @Transactional(readOnly = true)
    public TaskNotification.TaskNotificationEmail buildNotificationEmail(UUID taskId, UUID userId) {
        Task task = taskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + taskId + " not found"));
        AppUser user = task.getUser();

        return new TaskNotification.TaskNotificationEmail(
                user.getEmail(),
                user.getUsername(),
                task.getTitle(),
                task.getDescription()
        );
    }

    @Transactional
    public void markNotificationAsSent(UUID taskId, UUID userId) {
        Task task = taskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + taskId + " not found"));

        task.setNotificationStatus(NotificationStatus.SENT);
        task.setNotifiedAt(LocalDateTime.now());
        task.setLastNotificationError(null);
        taskRepository.save(task);
    }

    @Transactional
    public void markNotificationAsFailed(UUID taskId, String errorMessage) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + taskId + " not found"));

        task.setNotificationStatus(NotificationStatus.FAILED);
        task.setNotificationAttempts(task.getNotificationAttempts() + 1);
        task.setLastNotificationError(truncateError(errorMessage));
        taskRepository.save(task);
    }

    public TaskResponseDTO convertToResponseDTO(Task task) {
        return new TaskResponseDTO(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.isCompleted(),
            task.getDueAt(),
            task.getNotificationStatus()
        );

    }

    private NotificationStatus resolveInitialNotificationStatus(TaskRequestDTO dto) {
        if (Boolean.TRUE.equals(dto.notificationEnabled()) && dto.dueAt() != null) {
            return NotificationStatus.PENDING;
        }

        return NotificationStatus.NONE;
    }

    private void validateTaskRequest(TaskRequestDTO dto) {
        if (dto.title() == null || dto.title().trim().isBlank()) {
            throw new IllegalArgumentException("Task title is required.");
        }

        if (dto.title().length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Task title must be at most " + TITLE_MAX_LENGTH + " characters."
            );
        }

        if (dto.description() != null && dto.description().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Task description must be at most " + DESCRIPTION_MAX_LENGTH + " characters."
            );
        }
    }

    private String truncateError(String errorMessage) {
        String normalizedError = errorMessage == null ? "Unknown notification error" : errorMessage;

        if (normalizedError.length() <= 1000) {
            return normalizedError;
        }

        return normalizedError.substring(0, 1000);
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
