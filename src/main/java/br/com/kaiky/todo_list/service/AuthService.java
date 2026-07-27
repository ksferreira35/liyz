package br.com.kaiky.todo_list.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kaiky.todo_list.dto.RegisterRequestDTO;
import br.com.kaiky.todo_list.dto.UserResponseDTO;
import br.com.kaiky.todo_list.entity.AppUser;
import br.com.kaiky.todo_list.entity.Role;
import br.com.kaiky.todo_list.repository.AppUserRepository;

@Service
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService (
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDTO register(RegisterRequestDTO dto) {
        if(appUserRepository.existsByUsername(dto.username())) {
            throw new IllegalArgumentException("Username " + dto.username() + " already exists");
        }

        if(appUserRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email " + dto.email() + " already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(Role.USER);

        AppUser savedUser = appUserRepository.save(user);

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }
}
