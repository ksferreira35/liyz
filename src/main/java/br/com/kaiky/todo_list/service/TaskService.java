package br.com.kaiky.todo_list.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kaiky.todo_list.dto.ScheduleSectionDTO;
import br.com.kaiky.todo_list.dto.TaskNotificationMessage;
import br.com.kaiky.todo_list.dto.TaskRequestDTO;
import br.com.kaiky.todo_list.dto.TaskResponseDTO;
import br.com.kaiky.todo_list.dto.TaskStatusDTO;
import br.com.kaiky.todo_list.dto.TaskUpdateDTO;
import br.com.kaiky.todo_list.entity.AppUser;
import br.com.kaiky.todo_list.entity.NotificationStatus;
import br.com.kaiky.todo_list.entity.RecurrenceType;
import br.com.kaiky.todo_list.entity.Task;
import br.com.kaiky.todo_list.exception.ResourceNotFoundException;
import br.com.kaiky.todo_list.repository.TaskRepository;

@Service
public class TaskService {

    private static final int TITLE_MAX_LENGTH = 120;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final DateTimeFormatter SCHEDULE_DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "EEEE, MMM dd",
            Locale.ENGLISH
    );

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
        task.setRecurrenceType(resolveRecurrenceType(dto.recurrenceType()));

        Task saveTask = taskRepository.save(task);

        return convertToResponseDTO(saveTask);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponseDTO> getAllTasks(Pageable pageable) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Page<Task> tasks = taskRepository.findAllByUserId(userId, pageable);

        return tasks.map(this::convertToResponseDTO);
    }

    @Transactional(readOnly = true)
    public TaskResponseDTO findTaskById(UUID id) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                                  .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + id + " Not Found"));
        
        return convertToResponseDTO(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> findTasksByTitle(String title) {
        String normalizedTitle = normalizeTitle(title);
        UUID userId = authenticatedUserService.getCurrentUserId();

        return taskRepository
                .findByUser_IdAndTitleContainingIgnoreCase(userId, normalizedTitle)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    @Transactional
    public TaskResponseDTO updateTaskStatus(UUID id, TaskStatusDTO dto) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                                  .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + id + " Not Found"));
        
        boolean wasCompleted = task.isCompleted();
        task.setCompleted(dto.completed());
        
        Task savedTask = taskRepository.save(task);
        createNextOccurrenceIfNeeded(savedTask, wasCompleted);

        return convertToResponseDTO(savedTask);
    }

    @Transactional
    public TaskResponseDTO updateTask(UUID id, TaskUpdateDTO dto) {
        validateTaskUpdate(dto);
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + id + " Not Found"));

        task.setTitle(dto.title().trim());
        task.setDescription(dto.description());
        task.setDueAt(dto.dueAt());
        task.setRecurrenceType(resolveRecurrenceType(dto.recurrenceType()));
        resetNotificationState(task, Boolean.TRUE.equals(dto.notificationEnabled()));

        Task savedTask = taskRepository.save(task);

        return convertToResponseDTO(savedTask);
    }

    @Transactional
    public TaskResponseDTO updateTaskStatusByTitle(String title, TaskStatusDTO dto) {
        Task task = findSingleTaskByTitleForCurrentUser(title);

        boolean wasCompleted = task.isCompleted();
        task.setCompleted(dto.completed());
        Task savedTask = taskRepository.save(task);
        createNextOccurrenceIfNeeded(savedTask, wasCompleted);

        return convertToResponseDTO(savedTask);
    }
    
    @Transactional
    public void deleteTaskById(UUID id) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Task não encontrada"));

        taskRepository.delete(task);                        
    }

    @Transactional
    public TaskResponseDTO deleteTaskByTitle(String title) {
        Task task = findSingleTaskByTitleForCurrentUser(title);
        TaskResponseDTO response = convertToResponseDTO(task);

        taskRepository.delete(task);

        return response;
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> findNotificationTasks(List<NotificationStatus> statuses) {
        UUID userId = authenticatedUserService.getCurrentUserId();

        return taskRepository
                .findByUser_IdAndNotificationStatusInOrderByDueAtAsc(userId, statuses)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleSectionDTO> getSchedule(String filter) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        LocalDate today = LocalDate.now();
        String normalizedFilter = normalizeScheduleFilter(filter);

        return switch (normalizedFilter) {
            case "tomorrow" -> List.of(new ScheduleSectionDTO(
                    "Tomorrow",
                    findTasksBetween(userId, today.plusDays(1).atStartOfDay(), today.plusDays(2).atStartOfDay())
            ));
            case "week" -> groupScheduleByDate(
                    findTasksBetween(userId, today.atStartOfDay(), today.plusDays(7).atStartOfDay())
            );
            case "later" -> groupScheduleByDate(
                    taskRepository
                            .findByUser_IdAndDueAtGreaterThanEqualOrderByDueAtAsc(userId, today.plusDays(7).atStartOfDay())
                            .stream()
                            .map(this::convertToResponseDTO)
                            .toList()
            );
            case "no-date" -> List.of(new ScheduleSectionDTO(
                    "No date",
                    taskRepository
                            .findByUser_IdAndDueAtIsNullOrderByCreatedAtDesc(userId)
                            .stream()
                            .map(this::convertToResponseDTO)
                            .toList()
            ));
            default -> List.of(new ScheduleSectionDTO(
                    "Today",
                    findTasksBetween(userId, today.atStartOfDay(), today.plusDays(1).atStartOfDay())
            ));
        };
    }

    @Transactional
    public TaskResponseDTO retryNotification(UUID id) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + id + " Not Found"));

        if (task.getDueAt() == null) {
            throw new IllegalArgumentException("Task does not have a reminder time.");
        }

        task.setNotificationStatus(NotificationStatus.PENDING);
        task.setNotificationQueuedAt(null);
        task.setNotifiedAt(null);
        task.setLastNotificationError(null);

        return convertToResponseDTO(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDTO disableNotification(UUID id) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id: " + id + " Not Found"));

        task.setNotificationStatus(NotificationStatus.NONE);
        task.setNotificationQueuedAt(null);
        task.setNotifiedAt(null);
        task.setLastNotificationError(null);

        return convertToResponseDTO(taskRepository.save(task));
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
            task.getNotificationStatus(),
            task.getRecurrenceType()
        );

    }

    private NotificationStatus resolveInitialNotificationStatus(TaskRequestDTO dto) {
        if (Boolean.TRUE.equals(dto.notificationEnabled()) && dto.dueAt() != null) {
            return NotificationStatus.PENDING;
        }

        return NotificationStatus.NONE;
    }

    private RecurrenceType resolveRecurrenceType(RecurrenceType recurrenceType) {
        return recurrenceType == null ? RecurrenceType.NONE : recurrenceType;
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

    private void validateTaskUpdate(TaskUpdateDTO dto) {
        validateTaskRequest(new TaskRequestDTO(
                dto.title(),
                dto.description(),
                dto.dueAt(),
                dto.notificationEnabled(),
                dto.recurrenceType()
        ));
    }

    private void resetNotificationState(Task task, boolean notificationEnabled) {
        task.setNotificationAttempts(0);
        task.setNotificationQueuedAt(null);
        task.setNotifiedAt(null);
        task.setLastNotificationError(null);

        if (notificationEnabled && task.getDueAt() != null && !task.isCompleted()) {
            task.setNotificationStatus(NotificationStatus.PENDING);
            return;
        }

        task.setNotificationStatus(NotificationStatus.NONE);
    }

    private void createNextOccurrenceIfNeeded(Task completedTask, boolean wasCompleted) {
        if (wasCompleted || !completedTask.isCompleted()) {
            return;
        }

        RecurrenceType recurrenceType = resolveRecurrenceType(completedTask.getRecurrenceType());
        if (recurrenceType == RecurrenceType.NONE || completedTask.getDueAt() == null) {
            return;
        }

        Task nextTask = new Task(completedTask.getTitle(), completedTask.getDescription());
        nextTask.setUser(completedTask.getUser());
        nextTask.setDueAt(nextDueAt(completedTask.getDueAt(), recurrenceType));
        nextTask.setRecurrenceType(recurrenceType);
        nextTask.setNotificationStatus(
                completedTask.getNotificationStatus() == NotificationStatus.NONE
                        ? NotificationStatus.NONE
                        : NotificationStatus.PENDING
        );

        taskRepository.save(nextTask);
    }

    private LocalDateTime nextDueAt(LocalDateTime dueAt, RecurrenceType recurrenceType) {
        return switch (recurrenceType) {
            case DAILY -> dueAt.plusDays(1);
            case WEEKLY -> dueAt.plusWeeks(1);
            case MONTHLY -> dueAt.plusMonths(1);
            case NONE -> dueAt;
        };
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

    private String normalizeScheduleFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return "today";
        }

        return switch (filter.trim().toLowerCase(Locale.ENGLISH)) {
            case "today", "tomorrow", "week", "later", "no-date" -> filter.trim().toLowerCase(Locale.ENGLISH);
            default -> "today";
        };
    }

    private List<TaskResponseDTO> findTasksBetween(UUID userId, LocalDateTime start, LocalDateTime end) {
        return taskRepository
                .findByUser_IdAndDueAtBetweenOrderByDueAtAsc(userId, start, end)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    private List<ScheduleSectionDTO> groupScheduleByDate(List<TaskResponseDTO> tasks) {
        Map<LocalDate, List<TaskResponseDTO>> tasksByDate = tasks
                .stream()
                .collect(Collectors.groupingBy(
                        task -> task.dueAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return tasksByDate
                .entrySet()
                .stream()
                .map(entry -> new ScheduleSectionDTO(formatScheduleDate(entry.getKey()), entry.getValue()))
                .toList();
    }

    private String formatScheduleDate(LocalDate date) {
        return date.format(SCHEDULE_DATE_FORMATTER);
    }
}
