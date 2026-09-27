package com.example.jari.shared.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleApiException_returnsStatusAndBody() {
        ApiException ex = new ApiException(HttpStatus.CONFLICT, "DUPLICATE", "Already exists");

        ResponseEntity<ErrorResponse> response = handler.handleApiException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("DUPLICATE");
        assertThat(response.getBody().getMessage()).isEqualTo("Already exists");
    }

    @Test
    void handleValidation_returnsFieldErrors() throws NoSuchMethodException {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "email", "must be valid"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFieldErrors()).containsEntry("email", "must be valid");
    }

    @Test
    void handleValidation_usesDefaultMessageWhenNull() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", null, false, null, null, null));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getBody().getFieldErrors()).containsEntry("name", "Invalid value");
    }

    @Test
    void handleTypeMismatch_returnsBadRequest() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getValue()).thenReturn("not-a-uuid");
        when(ex.getMessage()).thenReturn("Failed to convert");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("INVALID_PARAMETER");
        assertThat(response.getBody().getMessage()).contains("id");
    }

    @Test
    void handleGeneric_returnsInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void handleGeneric_mapsPoolExhaustionTo503() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
            new RuntimeException("Connection is not available, request timed out after 5000ms."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getError()).isEqualTo("DB_POOL_EXHAUSTED");
    }

    @Test
    void handleGeneric_mapsHikariMessageTo503() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
            new RuntimeException("HikariDataSource - connection is not available"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getError()).isEqualTo("DB_POOL_EXHAUSTED");
    }

    @Test
    void handleGeneric_truncatesLongMessages() {
        String longMsg = "x".repeat(400);
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException(longMsg));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage().length()).isLessThanOrEqualTo(300);
    }

    @Test
    void handleGeneric_returnsNullOnClientAbort() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
            new RuntimeException("Broken pipe"));

        assertThat(response).isNull();
    }

    @Test
    void handleDataIntegrity_returnsConflict() {
        var ex = mock(org.springframework.dao.DataIntegrityViolationException.class);
        when(ex.getMostSpecificCause()).thenReturn(new RuntimeException("duplicate key"));

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError()).isEqualTo("CONFLICT");
    }

    @Test
    void handleCannotAcquireLock_returnsServiceUnavailable() {
        var ex = mock(org.springframework.dao.CannotAcquireLockException.class);
        when(ex.getMessage()).thenReturn("could not obtain lock");

        ResponseEntity<ErrorResponse> response = handler.handleLock(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getError()).isEqualTo("DB_LOCK");
    }

    @Test
    void handleDataAccessResource_returnsServiceUnavailable() {
        var ex = mock(org.springframework.dao.DataAccessResourceFailureException.class);
        when(ex.getMessage()).thenReturn("pool exhausted");

        ResponseEntity<ErrorResponse> response = handler.handleDataAccessResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getError()).isEqualTo("DB_UNAVAILABLE");
    }

    @Test
    void handleClientGone_doesNotThrow() {
        var ex = mock(org.springframework.web.context.request.async.AsyncRequestNotUsableException.class);
        when(ex.getMessage()).thenReturn("disconnected");
        handler.handleClientGone(ex);
    }

    @Test
    void handleIoException_ignoresBrokenPipe() throws Exception {
        handler.handleIoException(new java.io.IOException("Broken pipe"));
    }

    @Test
    void handleIoException_logsOtherIoErrors() throws Exception {
        handler.handleIoException(new java.io.IOException("disk full"));
    }
}
