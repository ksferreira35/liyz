package br.com.kaiky.todo_list.dto;

import java.time.LocalDateTime;

import br.com.kaiky.todo_list.entity.RecurrenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

public record TaskRequestDTO(
        @NotBlank(message = "Task title is required.")
        @Size(max = 120, message = "Task title must be at most 120 characters.")
        String title,
        @Size(max = 2000, message = "Task description must be at most 2000 characters.")
        String description,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime dueAt,
        Boolean notificationEnabled,
        RecurrenceType recurrenceType
) {
    public TaskRequestDTO(String title, String description) {
        this(title, description, null, false, RecurrenceType.NONE);
    }

    public TaskRequestDTO(String title, String description, LocalDateTime dueAt, Boolean notificationEnabled) {
        this(title, description, dueAt, notificationEnabled, RecurrenceType.NONE);
    }
}
