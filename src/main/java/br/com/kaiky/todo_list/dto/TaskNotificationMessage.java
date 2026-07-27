package br.com.kaiky.todo_list.dto;

import java.util.UUID;

public record TaskNotificationMessage(
        UUID taskId,
        UUID userId
) {
}
