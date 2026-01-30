package com.inventory.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(final InvalidRequestException ex) {
        final ErrorResponse error = ErrorResponse.builder()
            .id(null)
            .message(ex.getMessage())
            .errorCode(400)
            .retryable(false)
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(final ResourceNotFoundException ex) {
        final ErrorResponse error = ErrorResponse.builder()
            .id(null)
            .message(ex.getMessage())
            .errorCode(404)
            .retryable(false)
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ErrorResponse> handleInternalServer(final InternalServerException ex) {
        final ErrorResponse error = ErrorResponse.builder()
            .id(null)
            .message(ex.getMessage())
            .errorCode(500)
            .retryable(true)
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(final MethodArgumentNotValidException ex) {
        final String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Validation failed");
        
        final ErrorResponse error = ErrorResponse.builder()
            .id(null)
            .message(message)
            .errorCode(400)
            .retryable(false)
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(final NoHandlerFoundException ex) {
        final ErrorResponse error = ErrorResponse.builder()
            .id(null)
            .message("Endpoint not found: " + ex.getRequestURL())
            .errorCode(404)
            .retryable(false)
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGenericError(final RuntimeException ex) {
        String message = ex.getMessage();
        if (message.contains("Newer version") && message.contains("found in database")) {
            message = "Stock reservation failed due to concurrent access. Please try again.";
        }
        final ErrorResponse error = ErrorResponse.builder()
            .id(null)
            .message(message)
            .errorCode(500)
            .retryable(true)
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}