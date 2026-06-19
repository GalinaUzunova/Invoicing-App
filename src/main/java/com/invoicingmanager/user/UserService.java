package com.invoicingmanager.user;

import java.util.Locale;
import java.util.Objects;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
    }

    @Transactional
    public UserEntity register(RegisterUserDTO registerUserDTO) {
        String normalizedEmail = normalizeEmail(registerUserDTO.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        UserEntity user = new UserEntity();
        user.setFirstName(trimRequired(registerUserDTO.getFirstName(), "firstName"));
        user.setLastName(trimRequired(registerUserDTO.getLastName(), "lastName"));
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(trimRequired(registerUserDTO.getPassword(), "password")));
        user.setRole("ROLE_USER");
        user.setEnabled(true);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity getCurrentUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new UsernameNotFoundException("Authenticated user not found.");
        }

        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found."));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
