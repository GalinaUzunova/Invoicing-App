package com.invoicingmanager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(ResponseStatusException exception, HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpStatusCode statusCode = exception.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String message = Optional.ofNullable(exception.getReason())
                .filter(reason -> !reason.isBlank())
                .orElse(defaultMessage(statusCode));

        logRequestFailure(statusCode, request, message, exception);
        prepareErrorModel(model, response, statusCode, titleFor(status), message, request);
        return "error";
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class
    })
    public String handleBadRequest(Exception exception, HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = Optional.ofNullable(exception.getMessage())
                .filter(value -> !value.isBlank())
                .orElse("The request could not be processed.");

        log.warn("Bad request at {} {}: {}", request.getMethod(), request.getRequestURI(), message);
        prepareErrorModel(model, response, status, "Bad request", message, request);
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(DataIntegrityViolationException exception, HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "This action cannot be completed because related records still exist.";

        log.warn("Data integrity violation at {} {}: {}", request.getMethod(), request.getRequestURI(), rootCauseMessage(exception));
        prepareErrorModel(model, response, status, "Bad request", message, request);
        return "error";
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public String handleUserNotFound(UsernameNotFoundException exception, HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String message = Optional.ofNullable(exception.getMessage())
                .filter(value -> !value.isBlank())
                .orElse("User not found.");

        log.warn("User lookup failed at {} {}: {}", request.getMethod(), request.getRequestURI(), message);
        prepareErrorModel(model, response, status, "Not found", message, request);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpectedException(Exception exception, HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Unexpected server error at {} {}", request.getMethod(), request.getRequestURI(), exception);
        prepareErrorModel(model, response, status, "Something went wrong", "An unexpected problem occurred. Please try again.", request);
        return "error";
    }

    private void prepareErrorModel(Model model, HttpServletResponse response, HttpStatusCode statusCode, String title, String message, HttpServletRequest request) {
        response.setStatus(statusCode.value());
        model.addAttribute("pageTitle", title);
        model.addAttribute("statusCode", statusCode.value());
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        model.addAttribute("path", request.getRequestURI());
    }

    private void logRequestFailure(HttpStatusCode statusCode, HttpServletRequest request, String message, ResponseStatusException exception) {
        if (statusCode.is5xxServerError()) {
            log.error("Request failed with status {} at {} {}: {}", statusCode.value(), request.getMethod(), request.getRequestURI(), message, exception);
            return;
        }

        log.warn("Request failed with status {} at {} {}: {}", statusCode.value(), request.getMethod(), request.getRequestURI(), message);
    }

    private String titleFor(HttpStatus status) {
        if (status == null) {
            return "Request failed";
        }
        if (status == HttpStatus.NOT_FOUND) {
            return "Not found";
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return "Bad request";
        }
        if (status.is5xxServerError()) {
            return "Something went wrong";
        }
        return status.getReasonPhrase();
    }

    private String defaultMessage(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status == null ? "The request could not be processed." : status.getReasonPhrase();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return Optional.ofNullable(current.getMessage())
                .filter(message -> !message.isBlank())
                .orElse(current.getClass().getSimpleName());
    }
}
