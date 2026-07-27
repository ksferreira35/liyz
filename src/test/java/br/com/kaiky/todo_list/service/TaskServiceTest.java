package br.com.kaiky.todo_list.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void shouldCreateTaskSuccessfully() {
        TaskRequestDTO request = new TaskRequestDTO(
                "Learn Mockito",
                "Practicing writing unit tests."
        );
        UUID taskId = UUID.randomUUID();
        AppUser user = user(UUID.randomUUID());
        Task savedTask = task(
                taskId,
                request.title(),
                request.description(),
                false
        );

        when(authenticatedUserService.getCurrentUser()).thenReturn(user);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponseDTO response = taskService.createTask(request);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());

        assertThat(taskCaptor.getValue().getTitle()).isEqualTo(request.title());
        assertThat(taskCaptor.getValue().getDescription()).isEqualTo(request.description());
        assertThat(taskCaptor.getValue().isCompleted()).isFalse();
        assertThat(taskCaptor.getValue().getUser()).isSameAs(user);
        assertThat(response.id()).isEqualTo(taskId);
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.description()).isEqualTo(request.description());
        assertThat(response.completed()).isFalse();
        assertThat(response.recurrenceType()).isEqualTo(RecurrenceType.NONE);
    }

    @Test
    void shouldCreateTaskWithPendingNotificationWhenReminderIsEnabled() {
        LocalDateTime dueAt = LocalDateTime.now().plusHours(2);
        TaskRequestDTO request = new TaskRequestDTO(
                "Sleep",
                "Sleep warm",
                dueAt,
                true
        );
        UUID taskId = UUID.randomUUID();
        AppUser user = user(UUID.randomUUID());
        Task savedTask = task(
                taskId,
                request.title(),
                request.description(),
                false
        );
        savedTask.setDueAt(dueAt);
        savedTask.setNotificationStatus(NotificationStatus.PENDING);

        when(authenticatedUserService.getCurrentUser()).thenReturn(user);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponseDTO response = taskService.createTask(request);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());

        assertThat(taskCaptor.getValue().getDueAt()).isEqualTo(dueAt);
        assertThat(taskCaptor.getValue().getNotificationStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(response.dueAt()).isEqualTo(dueAt);
        assertThat(response.notificationStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void shouldCreateRecurringTask() {
        TaskRequestDTO request = new TaskRequestDTO(
                "Drink water",
                "Hydration routine.",
                null,
                false,
                RecurrenceType.DAILY
        );
        AppUser user = user(UUID.randomUUID());
        Task savedTask = task(UUID.randomUUID(), request.title(), request.description(), false);
        savedTask.setRecurrenceType(RecurrenceType.DAILY);

        when(authenticatedUserService.getCurrentUser()).thenReturn(user);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponseDTO response = taskService.createTask(request);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());

        assertThat(taskCaptor.getValue().getRecurrenceType()).isEqualTo(RecurrenceType.DAILY);
        assertThat(response.recurrenceType()).isEqualTo(RecurrenceType.DAILY);
    }

    @Test
    void shouldRejectTaskWhenDescriptionIsTooLong() {
        String longDescription = "a".repeat(2001);
        TaskRequestDTO request = new TaskRequestDTO(
                "Learn Mockito",
                longDescription
        );

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task description must be at most 2000 characters");

        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void shouldRejectTaskWhenTitleIsTooLong() {
        String longTitle = "a".repeat(121);
        TaskRequestDTO request = new TaskRequestDTO(
                longTitle,
                "Practicing writing unit tests."
        );

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task title must be at most 120 characters");

        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void shouldReturnPageOfTaskResponseDTO() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID userId = UUID.randomUUID();
        Task entity = task(
                UUID.randomUUID(),
                "Learn Mockito",
                "Practicing writing unit tests.",
                false
        );
        Page<Task> mockedPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findAllByUserId(userId, pageable)).thenReturn(mockedPage);

        Page<TaskResponseDTO> resultPage = taskService.getAllTasks(pageable);

        assertThat(resultPage.getTotalElements()).isEqualTo(1);
        assertThat(resultPage.getContent())
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.id()).isEqualTo(entity.getId());
                    assertThat(response.title()).isEqualTo(entity.getTitle());
                    assertThat(response.description()).isEqualTo(entity.getDescription());
                    assertThat(response.completed()).isFalse();
                });
    }

    @Test
    void shouldFindTaskByIdSuccessfully() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Task mockedTask = task(
                taskId,
                "Learn Mockito",
                "Practicing writing unit tests.",
                false
        );

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(mockedTask));

        TaskResponseDTO result = taskService.findTaskById(taskId);

        assertThat(result.id()).isEqualTo(taskId);
        assertThat(result.title()).isEqualTo(mockedTask.getTitle());
        assertThat(result.description()).isEqualTo(mockedTask.getDescription());
        assertThat(result.completed()).isFalse();
    }

    @Test
    void shouldFindTasksByTitleForCurrentUser() {
        UUID userId = UUID.randomUUID();
        Task mockedTask = task(
                UUID.randomUUID(),
                "Assistir TV",
                "Relax after work.",
                false
        );

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByUser_IdAndTitleContainingIgnoreCase(userId, "TV"))
                .thenReturn(List.of(mockedTask));

        List<TaskResponseDTO> result = taskService.findTasksByTitle(" TV ");

        assertThat(result)
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.id()).isEqualTo(mockedTask.getId());
                    assertThat(response.title()).isEqualTo("Assistir TV");
                    assertThat(response.description()).isEqualTo("Relax after work.");
                    assertThat(response.completed()).isFalse();
                });
    }

    @Test
    void shouldThrowWhenTaskIsNotFoundById() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findTaskById(taskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(taskId.toString());
    }

    @Test
    void shouldUpdateTaskStatusSuccessfully() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Task mockedTask = task(taskId, "Learn Mockito", null, false);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(mockedTask));
        when(taskRepository.save(mockedTask)).thenReturn(mockedTask);

        taskService.updateTaskStatus(taskId, new TaskStatusDTO(true));

        assertThat(mockedTask.isCompleted()).isTrue();
        verify(taskRepository).save(mockedTask);
    }

    @Test
    void shouldCreateNextOccurrenceWhenCompletingRecurringTask() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AppUser user = user(userId);
        LocalDateTime dueAt = LocalDateTime.now().plusHours(1);
        Task recurringTask = task(taskId, "Take medicine", "Daily medicine.", false);
        recurringTask.setUser(user);
        recurringTask.setDueAt(dueAt);
        recurringTask.setNotificationStatus(NotificationStatus.PENDING);
        recurringTask.setRecurrenceType(RecurrenceType.DAILY);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(recurringTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.updateTaskStatus(taskId, new TaskStatusDTO(true));

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, org.mockito.Mockito.times(2)).save(taskCaptor.capture());

        Task nextTask = taskCaptor.getAllValues().get(1);
        assertThat(nextTask.getTitle()).isEqualTo("Take medicine");
        assertThat(nextTask.getDueAt()).isEqualTo(dueAt.plusDays(1));
        assertThat(nextTask.getNotificationStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(nextTask.getRecurrenceType()).isEqualTo(RecurrenceType.DAILY);
        assertThat(nextTask.isCompleted()).isFalse();
    }

    @Test
    void shouldUpdateTaskDetailsSuccessfully() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime dueAt = LocalDateTime.now().plusDays(1);
        Task mockedTask = task(taskId, "Old title", "Old description", false);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(mockedTask));
        when(taskRepository.save(mockedTask)).thenReturn(mockedTask);

        TaskResponseDTO response = taskService.updateTask(
                taskId,
                new TaskUpdateDTO(
                        "New title",
                        "New description",
                        dueAt,
                        true,
                        RecurrenceType.WEEKLY
                )
        );

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.dueAt()).isEqualTo(dueAt);
        assertThat(response.notificationStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(response.recurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
        verify(taskRepository).save(mockedTask);
    }

    @Test
    void shouldUpdateTaskStatusByTitleSuccessfully() {
        UUID userId = UUID.randomUUID();
        Task mockedTask = task(UUID.randomUUID(), "Assistir TV", null, false);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByUser_IdAndTitleIgnoreCase(userId, "Assistir TV"))
                .thenReturn(List.of(mockedTask));
        when(taskRepository.save(mockedTask)).thenReturn(mockedTask);

        TaskResponseDTO response = taskService.updateTaskStatusByTitle(
                " Assistir TV ",
                new TaskStatusDTO(true)
        );

        assertThat(mockedTask.isCompleted()).isTrue();
        assertThat(response.completed()).isTrue();
        verify(taskRepository).save(mockedTask);
    }

    @Test
    void shouldThrowWhenMoreThanOneTaskMatchesTitle() {
        UUID userId = UUID.randomUUID();
        Task firstTask = task(UUID.randomUUID(), "Study Java", null, false);
        Task secondTask = task(UUID.randomUUID(), "Study Java docs", null, false);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByUser_IdAndTitleIgnoreCase(userId, "Study"))
                .thenReturn(List.of());
        when(taskRepository.findByUser_IdAndTitleContainingIgnoreCase(userId, "Study"))
                .thenReturn(List.of(firstTask, secondTask));

        assertThatThrownBy(() -> taskService.updateTaskStatusByTitle("Study", new TaskStatusDTO(true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("More than one task matched");
    }

    @Test
    void shouldThrowWhenUpdatingMissingTask() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTaskStatus(taskId, new TaskStatusDTO(true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(taskId.toString());
        verify(taskRepository).findByIdAndUserId(taskId, userId);
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void shouldDeleteTaskSuccessfully() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Task mockedTask = task(taskId, "Learn Mockito", null, false);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(mockedTask));

        taskService.deleteTaskById(taskId);

        verify(taskRepository).delete(mockedTask);
    }

    @Test
    void shouldDeleteTaskByTitleSuccessfully() {
        UUID userId = UUID.randomUUID();
        Task mockedTask = task(UUID.randomUUID(), "Assistir TV", null, false);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByUser_IdAndTitleIgnoreCase(userId, "Assistir TV"))
                .thenReturn(List.of(mockedTask));

        TaskResponseDTO response = taskService.deleteTaskByTitle("Assistir TV");

        assertThat(response.id()).isEqualTo(mockedTask.getId());
        assertThat(response.title()).isEqualTo("Assistir TV");
        verify(taskRepository).delete(mockedTask);
    }

    @Test
    void shouldFindDueNotifications() {
        LocalDateTime now = LocalDateTime.now();
        Task dueTask = task(UUID.randomUUID(), "Study", null, false);
        dueTask.setDueAt(now.minusMinutes(1));
        dueTask.setNotificationStatus(NotificationStatus.PENDING);
        Pageable expectedPageable = PageRequest.of(0, 100);

        when(taskRepository.findDueNotifications(now, 3, expectedPageable))
                .thenReturn(List.of(dueTask));

        List<TaskResponseDTO> result = taskService.findDueNotifications(now, 100, 3);

        assertThat(result)
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.id()).isEqualTo(dueTask.getId());
                    assertThat(response.notificationStatus()).isEqualTo(NotificationStatus.PENDING);
                });
    }

    @Test
    void shouldReturnTodayScheduleForCurrentUser() {
        UUID userId = UUID.randomUUID();
        LocalDateTime dueAt = LocalDateTime.now().withHour(14).withMinute(0);
        Task task = task(UUID.randomUUID(), "Review dashboard", "Check layout polish.", false);
        task.setDueAt(dueAt);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByUser_IdAndDueAtBetweenOrderByDueAtAsc(
                eq(userId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(task));

        var sections = taskService.getSchedule("today");

        assertThat(sections)
                .singleElement()
                .satisfies(section -> {
                    assertThat(section.label()).isEqualTo("Today");
                    assertThat(section.tasks())
                            .singleElement()
                            .satisfies(response -> {
                                assertThat(response.title()).isEqualTo("Review dashboard");
                                assertThat(response.dueAt()).isEqualTo(dueAt);
                            });
                });
    }

    @Test
    void shouldReturnNoDateScheduleForCurrentUser() {
        UUID userId = UUID.randomUUID();
        Task task = task(UUID.randomUUID(), "Clean backlog", null, false);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByUser_IdAndDueAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(task));

        var sections = taskService.getSchedule("no-date");

        assertThat(sections)
                .singleElement()
                .satisfies(section -> {
                    assertThat(section.label()).isEqualTo("No date");
                    assertThat(section.tasks())
                            .singleElement()
                            .extracting("title")
                            .isEqualTo("Clean backlog");
                });
    }

    @Test
    void shouldMarkNotificationAsQueued() {
        UUID taskId = UUID.randomUUID();
        AppUser user = user(UUID.randomUUID());
        Task task = task(taskId, "Study", null, false);
        task.setUser(user);
        task.setNotificationStatus(NotificationStatus.PENDING);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        var message = taskService.markNotificationAsQueued(taskId);

        assertThat(task.getNotificationStatus()).isEqualTo(NotificationStatus.QUEUED);
        assertThat(task.getNotificationQueuedAt()).isNotNull();
        assertThat(message.taskId()).isEqualTo(taskId);
        assertThat(message.userId()).isEqualTo(user.getId());
        verify(taskRepository).save(task);
    }

    @Test
    void shouldThrowWhenDeletingMissingTask() {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTaskById(taskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
        verify(taskRepository).findByIdAndUserId(taskId, userId);
        verifyNoMoreInteractions(taskRepository);
    }

    private static Task task(
            UUID id,
            String title,
            String description,
            boolean completed
    ) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription(description);
        task.setCompleted(completed);
        return task;
    }

    private static AppUser user(UUID id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("kaiky");
        user.setEmail("kaiky@example.com");
        return user;
    }
}
