package com.workintech.s19challenge.exceptions;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(
            ResponseStatusException exception) {

        int status = exception.getStatusCode().value();

        String message = exception.getReason() == null
            ? "Request failed"
            : exception.getReason();

        return ResponseEntity.status(status).body(
            Map.of(
                "status", status,
                "message", message
            )
        );
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, Object>> handleValidation(
            Exception exception) {

        return badRequest("Invalid request data");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJson(
            HttpMessageNotReadableException exception) {

        return badRequest("Invalid JSON request");
    }

    private ResponseEntity<Map<String, Object>> badRequest(
            String message) {

        return ResponseEntity.badRequest().body(
            Map.of(
                "status", 400,
                "message", message
            )
        );
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleParameters(
            Exception exception) {

        return badRequest("Invalid request parameter");
    }

    @ExceptionHandler({
        HttpRequestMethodNotSupportedException.class,
        HttpMediaTypeNotSupportedException.class,
        NoResourceFoundException.class
    })
    public ResponseEntity<Map<String, Object>> handleRequestMapping(
            Exception exception) {

        ErrorResponse errorResponse = (ErrorResponse) exception;

        int status = errorResponse.getStatusCode().value();

        return ResponseEntity
            .status(status)
            .headers(errorResponse.getHeaders())
            .body(
                Map.of(
                    "status", status,
                    "message", HttpStatus.valueOf(status)
                        .getReasonPhrase()
                )
            );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            DataIntegrityViolationException exception) {

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                Map.of(
                    "status", 409,
                    "message", "Data conflict"
                )
            );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyDeleted(
            OptimisticLockingFailureException exception) {

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                Map.of(
                    "status", 404,
                    "message", "Record not found"
                )
            );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception exception) {

        log.error("Unexpected error", exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                Map.of(
                    "status", 500,
                    "message", "Internal server error"
                )
            );
    }
}
