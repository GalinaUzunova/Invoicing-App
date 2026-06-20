package com.invoicingmanager.user;

import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
    }

    @Transactional
    public UserEntity register(RegisterUserDTO registerUserDTO) {
        RegisterUserDTO requiredRegisterUserDTO = requireArgument(registerUserDTO, "register user");
        String normalizedEmail = normalizeRequiredEmail(requiredRegisterUserDTO.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Registration failed because email {} already exists", normalizedEmail);
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        UserEntity user = new UserEntity();
        user.setFirstName(trimRequired(requiredRegisterUserDTO.getFirstName(), "firstName"));
        user.setLastName(trimRequired(requiredRegisterUserDTO.getLastName(), "lastName"));
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(trimRequired(requiredRegisterUserDTO.getPassword(), "password")));
        user.setRole("ROLE_USER");
        user.setEnabled(true);

        userRepository.save(user);
        return user;
    }

    @Transactional(readOnly = true)
    public UserEntity getCurrentUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            log.warn("Authenticated user lookup failed because principal email was blank");
            throw new UsernameNotFoundException("Authenticated user not found.");
        }

        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Authenticated user {} was not found", normalizedEmail);
                    return new UsernameNotFoundException("Authenticated user not found.");
                });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequiredEmail(String email) {
        return trimRequired(email, "email").toLowerCase(Locale.ROOT);
    }

    private String trimRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private <T> T requireArgument(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
