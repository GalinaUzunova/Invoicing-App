package com.invoicingmanager.security;

import com.invoicingmanager.user.UserEntity;
import com.invoicingmanager.user.UserRepository;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(@NotNull String email) {
        String normalizedEmail = Objects.requireNonNull(email, "email must not be null").trim();
        if (normalizedEmail.isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }

        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(user.getRole())
                .disabled(!user.isEnabled())
                .build();
    }
}
