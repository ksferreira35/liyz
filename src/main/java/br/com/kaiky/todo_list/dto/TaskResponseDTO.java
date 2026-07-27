package br.com.kaiky.todo_list.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.kaiky.todo_list.entity.NotificationStatus;
import br.com.kaiky.todo_list.entity.RecurrenceType;

public record TaskResponseDTO(
        UUID id,
        String title,
        String description,
        boolean completed,
        LocalDateTime dueAt,
        NotificationStatus notificationStatus,
        RecurrenceType recurrenceType
) {
    
}
