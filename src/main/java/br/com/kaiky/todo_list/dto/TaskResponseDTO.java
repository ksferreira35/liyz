package br.com.kaiky.todo_list.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.kaiky.todo_list.entity.NotificationStatus;

public record TaskResponseDTO(
        UUID id,
        String title,
        String description,
        boolean completed,
        LocalDateTime dueAt,
        NotificationStatus notificationStatus
) {
    
}
