package com.invoicingmanager.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.invoicingmanager.security.SecurityConfig;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithAnonymousUser
    void registerPageIsPublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerUserDTO"));
    }

    @Test
    @WithAnonymousUser
    void postRegisterRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/register")
                        .param("firstName", "Grace")
                        .param("lastName", "Owner")
                        .param("email", "owner@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isForbidden());

        verify(userService, never()).register(any());
    }

    @Test
    @WithAnonymousUser
    void postRegisterWithBlankFieldsReturnsRegisterFormWithValidationErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .with(Objects.requireNonNull(csrf(), "csrf post processor must not be null")))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerUserDTO", "firstName"))
                .andExpect(model().attributeHasFieldErrors("registerUserDTO", "lastName"))
                .andExpect(model().attributeHasFieldErrors("registerUserDTO", "email"))
                .andExpect(model().attributeHasFieldErrors("registerUserDTO", "password"))
                .andExpect(model().attributeHasFieldErrors("registerUserDTO", "confirmPassword"));

        verify(userService, never()).register(any());
    }

    @Test
    @WithAnonymousUser
    void postRegisterWithMismatchedPasswordsReturnsRegisterFormWithPasswordConfirmedError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(Objects.requireNonNull(csrf(), "csrf post processor must not be null"))
                        .param("firstName", "Grace")
                        .param("lastName", "Owner")
                        .param("email", "owner@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "different123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerUserDTO", "passwordConfirmed"));

        verify(userService, never()).register(any());
    }

    @Test
    @WithAnonymousUser
    void postRegisterWithShortPasswordReturnsRegisterFormWithValidationErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .with(Objects.requireNonNull(csrf(), "csrf post processor must not be null"))
                        .param("firstName", "Grace")
                        .param("lastName", "Owner")
                        .param("email", "owner@example.com")
                        .param("password", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerUserDTO", "password"))
                .andExpect(model().attributeHasFieldErrors("registerUserDTO", "confirmPassword"));

        verify(userService, never()).register(any());
    }
}
