package br.com.kaiky.todo_list.dto;

import java.util.List;

public record ScheduleSectionDTO(
        String label,
        List<TaskResponseDTO> tasks
) {
}
