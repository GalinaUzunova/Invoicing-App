package com.invoicingmanager.user;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
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
            bindingResult.rejectValue("email", "email.duplicate", exception.getMessage());
            return "auth/register";
        }

        return "redirect:/login?registered";
    }
}
