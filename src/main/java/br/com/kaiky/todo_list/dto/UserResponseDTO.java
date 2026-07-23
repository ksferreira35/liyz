package br.com.kaiky.todo_list.dto;

import br.com.kaiky.todo_list.entity.Role;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String username,
        String email,
        Role role
) {
}
