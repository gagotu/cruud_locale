package com.exprivia.nest.cruud.exception.handler;

import com.exprivia.nest.cruud.exception.ApiError;
import com.exprivia.nest.cruud.exception.DuplicateNameException;
import com.exprivia.nest.cruud.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

/**
 * Global Exception Handler for project
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * method handle DuplicateNamePropertiesException
     * @param ex to throw
     * @return response entity status
     */
    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<ApiError> handleDuplicateNamePropertiesException(DuplicateNameException ex, HttpServletRequest request) {
        log.error(ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        log.error(ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        log.error(ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error(ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status, Exception ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request != null ? request.getRequestURI() : null
        );
        return ResponseEntity.status(status).body(error);
    }

}
