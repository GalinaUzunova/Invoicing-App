package com.invoicingmanager.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserService userService = new UserService(userRepository, passwordEncoder);
        UserEntity user = userService.register(registerUserDTO);

        assertThat(user.getEmail()).isEqualTo("owner@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getRole()).isEqualTo("ROLE_USER");
        assertThat(user.isEnabled()).isTrue();

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-password");
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
        verify(userRepository, never()).save(any());
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
}
