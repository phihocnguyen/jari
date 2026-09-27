package com.example.jari.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.warn("API exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(
            ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
                (a, b) -> a
            ));
        return ResponseEntity.badRequest().body(
            ErrorResponse.builder()
                .error("VALIDATION_FAILED")
                .message("Request validation failed")
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parameter type mismatch: {} - {}", ex.getName(), ex.getMessage());
        return ResponseEntity.badRequest().body(
            ErrorResponse.builder()
                .error("INVALID_PARAMETER")
                .message("Invalid parameter '" + ex.getName() + "': " + ex.getValue())
                .timestamp(Instant.now())
                .build()
        );
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessResource(DataAccessResourceFailureException ex) {
        log.error("DB resource failure (often pool exhaustion): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse.builder()
                .error("DB_UNAVAILABLE")
                .message("Database connection unavailable (pool exhausted or DB down)")
                .timestamp(Instant.now())
                .build()
        );
    }

    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<ErrorResponse> handleLock(CannotAcquireLockException ex) {
        log.warn("Could not acquire DB lock: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse.builder()
                .error("DB_LOCK")
                .message("Could not acquire database lock")
                .timestamp(Instant.now())
                .build()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse.builder()
                .error("CONFLICT")
                .message("Resource conflict (duplicate or constraint violation)")
                .timestamp(Instant.now())
                .build()
        );
    }

    /**
     * Client closed the socket (k6 timeout / connection reset) while the server was still
     * writing. Not an application bug — do not try to write an error body.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientGone(AsyncRequestNotUsableException ex) {
        log.debug("Client disconnected before response completed: {}", ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public void handleIoException(IOException ex) {
        if (isClientAbort(ex)) {
            log.debug("Client aborted connection: {}", ex.getMessage());
            return;
        }
        log.error("IO exception", ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        if (isClientAbort(ex)) {
            log.debug("Client disconnected: {}", ex.toString());
            return null;
        }
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String rootMsg = root.getMessage() != null ? root.getMessage() : ex.getMessage();
        if (rootMsg != null && (rootMsg.contains("Connection is not available")
                || rootMsg.contains("HikariDataSource")
                || root.getClass().getName().contains("SQLTransientConnection"))) {
            log.error("Connection pool exhausted: {}", rootMsg);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ErrorResponse.builder()
                    .error("DB_POOL_EXHAUSTED")
                    .message("Database connection pool exhausted")
                    .timestamp(Instant.now())
                    .build()
            );
        }
        log.error("Unhandled exception", ex);
        String detail = root.getClass().getSimpleName()
            + (rootMsg != null && !rootMsg.isBlank() ? ": " + rootMsg : "");
        if (detail.length() > 300) {
            detail = detail.substring(0, 300);
        }
        return ResponseEntity.internalServerError().body(
            ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message(detail)
                .timestamp(Instant.now())
                .build()
        );
    }

    private static boolean isClientAbort(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            String name = t.getClass().getName();
            String msg = t.getMessage() != null ? t.getMessage() : "";
            if (name.contains("ClientAbortException")
                    || name.contains("AsyncRequestNotUsableException")
                    || msg.contains("Broken pipe")
                    || msg.contains("Connection reset by peer")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
