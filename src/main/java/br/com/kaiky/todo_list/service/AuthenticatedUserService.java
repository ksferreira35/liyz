package br.com.kaiky.todo_list.service;

import java.util.UUID;

import br.com.kaiky.todo_list.entity.AppUser;
import br.com.kaiky.todo_list.repository.AppUserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {

    private final AppUserRepository appUserRepository;

    public AuthenticatedUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authenticated user not found"
            );
        }

        return appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Authenticated user not found"
                ));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
