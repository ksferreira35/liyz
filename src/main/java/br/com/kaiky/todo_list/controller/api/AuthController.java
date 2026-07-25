package br.com.kaiky.todo_list.controller.api;

import br.com.kaiky.todo_list.dto.RegisterRequestDTO;
import br.com.kaiky.todo_list.dto.UserResponseDTO;
import br.com.kaiky.todo_list.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(
            @RequestBody @Valid RegisterRequestDTO dto
    ) {
        UserResponseDTO user = authService.register(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(user);
    }
}
