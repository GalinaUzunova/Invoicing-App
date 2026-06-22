package com.invoicingmanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoicingmanager.user.AuthController;
import com.invoicingmanager.user.RegisterUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Model model;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/customers/99");
        response = new MockHttpServletResponse();
        model = new ExtendedModelMap();
    }

    @Test
    void handleResponseStatusExceptionUsesReasonAndNotFoundTitle() {
        String view = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found."),
                request,
                response,
                model
        );

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(model.getAttribute("statusCode")).isEqualTo(404);
        assertThat(model.getAttribute("errorTitle")).isEqualTo("Not found");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Customer not found.");
        assertThat(model.getAttribute("path")).isEqualTo("/customers/99");
    }

    @Test
    void handleResponseStatusExceptionFallsBackToDefaultMessageWhenReasonMissing() {
        String view = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST),
                request,
                response,
                model
        );

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(model.getAttribute("errorTitle")).isEqualTo("Bad request");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Bad Request");
    }

    @Test
    void handleIllegalArgumentExceptionReturnsBadRequestErrorPage() {
        String view = handler.handleBadRequest(
                new IllegalArgumentException("Customer phone is required."),
                request,
                response,
                model
        );

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(model.getAttribute("errorTitle")).isEqualTo("Bad request");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Customer phone is required.");
    }

    @Test
    void handleMethodArgumentTypeMismatchExceptionReturnsBadRequestErrorPage() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                "id",
                null,
                new NumberFormatException("For input string: \"abc\"")
        );

        String view = handler.handleBadRequest(exception, request, response, model);

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(model.getAttribute("errorMessage")).asString().contains("abc");
    }

    @Test
    void handleMethodArgumentNotValidExceptionReturnsBadRequestErrorPage() throws NoSuchMethodException {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        BindingResult bindingResult = new BeanPropertyBindingResult(registerUserDTO, "registerUserDTO");
        bindingResult.rejectValue("email", "NotBlank", "Email is required.");
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(
                        AuthController.class.getDeclaredMethod(
                                "register",
                                RegisterUserDTO.class,
                                BindingResult.class,
                                Model.class
                        ),
                        0
                ),
                bindingResult
        );

        String view = handler.handleBadRequest(exception, request, response, model);

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(model.getAttribute("errorMessage")).asString().contains("Validation failed");
    }

    @Test
    void handleDataIntegrityViolationReturnsSafeMessage() {
        String view = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("foreign key constraint fails"),
                request,
                response,
                model
        );

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(model.getAttribute("errorMessage"))
                .isEqualTo("This action cannot be completed because related records still exist.");
    }

    @Test
    void handleUsernameNotFoundExceptionReturnsNotFoundErrorPage() {
        String view = handler.handleUserNotFound(
                new UsernameNotFoundException("Authenticated user not found."),
                request,
                response,
                model
        );

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(model.getAttribute("errorTitle")).isEqualTo("Not found");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Authenticated user not found.");
    }

    @Test
    void handleUnexpectedExceptionReturnsGenericServerErrorMessage() {
        String view = handler.handleUnexpectedException(
                new RuntimeException("database connection failed"),
                request,
                response,
                model
        );

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(model.getAttribute("errorTitle")).isEqualTo("Something went wrong");
        assertThat(model.getAttribute("errorMessage"))
                .isEqualTo("An unexpected problem occurred. Please try again.");
    }
}
