package com.exprivia.nest.cruud.exception.handler;

import com.exprivia.nest.cruud.exception.DuplicateNameException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
    public ResponseEntity<String> handleDuplicateNamePropertiesException(DuplicateNameException ex) {
        log.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

}
