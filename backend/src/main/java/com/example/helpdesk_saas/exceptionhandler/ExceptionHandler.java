package com.example.helpdesk_saas.exceptionhandler;

import com.example.helpdesk_saas.exception.InvalidCredentialsException;
import com.example.helpdesk_saas.exception.UserAlreadyExistException;
import com.example.helpdesk_saas.exception.UserNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class ExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ExceptionHandler.class);

    @org.springframework.web.bind.annotation.ExceptionHandler(
            InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentialsException(
            InvalidCredentialsException ex) {

        return new ResponseEntity<>(
                "Error: " + ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(
            UserAlreadyExistException.class)
    public ResponseEntity<String> handleUserAlreadyExistException(
            UserAlreadyExistException ex) {

        return new ResponseEntity<>(
                "Error: " + ex.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(
            UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException(
            UserNotFoundException ex) {

        return new ResponseEntity<>(
                "Error: " + ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(
            MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationError(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .filter(error -> "password".equals(error.getField()))
                .map(error -> error.getDefaultMessage())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse("Invalid registration details.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message);
    }

    // Generic handler for unexpected backend failures.
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleInternalServerError(
            Exception ex,
            HttpServletRequest request) {

        logger.error(
                "Internal server error during {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal Server Error. Please try again later.");
    }
}