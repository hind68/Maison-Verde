package com.portfolio.hotel.shared.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ReservationConflictException exception) {
        return response(HttpStatus.CONFLICT, "RESERVATION_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiError> handleResourceConflict(ResourceConflictException exception) {
        return response(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(BusinessValidationException exception) {
        return response(HttpStatus.BAD_REQUEST, "BUSINESS_VALIDATION", exception.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationFailedException exception) {
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ApiError body = new ApiError(Instant.now(), 400, "VALIDATION_ERROR", "La requête est invalide.", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiError> handleRequestValidation(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Les paramètres fournis sont invalides.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Une erreur interne est survenue.");
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message));
    }
}
