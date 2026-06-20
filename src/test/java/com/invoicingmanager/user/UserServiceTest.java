package com.invoicingmanager.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.invocation.Invocation;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void registerHashesPasswordAndNormalizesEmail() {
        RegisterUserDTO registerUserDTO = registerUserDTO();
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        UserService userService = new UserService(userRepository, passwordEncoder);
        UserEntity user = userService.register(registerUserDTO);

        assertThat(user.getEmail()).isEqualTo("owner@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getRole()).isEqualTo("ROLE_USER");
        assertThat(user.isEnabled()).isTrue();

        assertThat(savedUser().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void registerRejectsDuplicateEmailBeforeHashingPassword() {
        RegisterUserDTO registerUserDTO = registerUserDTO();
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(true);

        UserService userService = new UserService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> userService.register(registerUserDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email already exists");

        verify(passwordEncoder, never()).encode(any());
        assertThat(mockingDetails(userRepository).getInvocations())
                .noneMatch(invocation -> "save".equals(invocation.getMethod().getName()));
    }

    @Test
    void getCurrentUserNormalizesEmailAndReturnsUser() {
        UserEntity user = new UserEntity();
        user.setEmail("owner@example.com");
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));

        UserService userService = new UserService(userRepository, passwordEncoder);

        assertThat(userService.getCurrentUser(" Owner@Example.com ")).isSameAs(user);
    }

    @Test
    void getCurrentUserRejectsBlankOrMissingUser() {
        UserService userService = new UserService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> userService.getCurrentUser(" "))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Authenticated user not found");

        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getCurrentUser("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Authenticated user not found");
    }

    @Test
    void registerRejectsNullDto() {
        UserService userService = new UserService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> userService.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("register user is required");
    }

    @Test
    void registerRejectsNullRequiredEmailBeforeRepositoryLookup() {
        RegisterUserDTO registerUserDTO = registerUserDTO();
        registerUserDTO.setEmail(null);

        assertThatThrownBy(() -> new UserService(userRepository, passwordEncoder).register(registerUserDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email is required");

        assertThat(org.mockito.Mockito.mockingDetails(userRepository).getInvocations()).isEmpty();
    }

    private RegisterUserDTO registerUserDTO() {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setFirstName("Grace");
        registerUserDTO.setLastName("Owner");
        registerUserDTO.setEmail(" Owner@Example.com ");
        registerUserDTO.setPassword("password123");
        registerUserDTO.setConfirmPassword("password123");
        return registerUserDTO;
    }

    private UserEntity savedUser() {
        return mockingDetails(userRepository).getInvocations().stream()
                .filter(invocation -> "save".equals(invocation.getMethod().getName()))
                .findFirst()
                .map(this::userArgument)
                .orElseThrow(() -> new AssertionError("Expected user to be saved."));
    }

    private UserEntity userArgument(Invocation invocation) {
        Object argument = invocation.getArgument(0);
        assertThat(argument).isInstanceOf(UserEntity.class);
        return (UserEntity) Objects.requireNonNull(argument, "saved user must not be null");
    }
}
