package br.com.kaiky.todo_list.service;

import br.com.kaiky.todo_list.dto.TaskRequestDTO;
import br.com.kaiky.todo_list.dto.TaskResponseDTO;
import br.com.kaiky.todo_list.dto.TaskStatusDTO;
import br.com.kaiky.todo_list.entity.AppUser;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

        taskService.updateTaskStatus(taskId, new TaskStatusDTO(true));

        assertThat(mockedTask.isCompleted()).isTrue();
        verify(taskRepository).save(mockedTask);
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
