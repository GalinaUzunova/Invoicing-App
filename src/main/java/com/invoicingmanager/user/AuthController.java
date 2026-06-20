package com.invoicingmanager.user;

import jakarta.validation.Valid;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Slf4j
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Login");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("pageTitle", "Create Account");
        model.addAttribute("registerUserDTO", new RegisterUserDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegisterUserDTO registerUserDTO,
            BindingResult bindingResult,
            Model model
    ) {
        model.addAttribute("pageTitle", "Create Account");

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(registerUserDTO);
        } catch (IllegalArgumentException exception) {
            String message = exceptionMessage(exception, "Unable to create account.");
            log.warn("Registration failed for email {}: {}", registerUserDTO.getEmail(), message);
            bindingResult.rejectValue("email", "email.duplicate", Objects.requireNonNull(message, "message must not be null"));
            return "auth/register";
        }

        return "redirect:/login?registered";
    }

    private String exceptionMessage(RuntimeException exception, String fallback) {
        return Optional.ofNullable(exception.getMessage())
                .filter(message -> !message.isBlank())
                .orElse(fallback);
    }
}
