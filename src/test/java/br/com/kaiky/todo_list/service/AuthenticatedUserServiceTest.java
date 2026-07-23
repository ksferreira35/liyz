package br.com.kaiky.todo_list.service;

import br.com.kaiky.todo_list.entity.AppUser;
import br.com.kaiky.todo_list.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserFromSecurityContext() {
        AppUser user = user(UUID.randomUUID(), "kaiky");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        user.getUsername(),
                        "password",
                        null
                )
        );
        AuthenticatedUserService service = new AuthenticatedUserService(appUserRepository);

        when(appUserRepository.findByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));

        AppUser result = service.getCurrentUser();

        assertThat(result).isSameAs(user);
        assertThat(service.getCurrentUserId()).isEqualTo(user.getId());
    }

    @Test
    void shouldThrowWhenAuthenticationIsMissing() {
        AuthenticatedUserService service = new AuthenticatedUserService(appUserRepository);

        assertThatThrownBy(service::getCurrentUser)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void shouldThrowWhenAuthenticatedUserDoesNotExist() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "missing",
                        "password",
                        null
                )
        );
        AuthenticatedUserService service = new AuthenticatedUserService(appUserRepository);

        when(appUserRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(service::getCurrentUser)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private static AppUser user(UUID id, String username) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }
}
