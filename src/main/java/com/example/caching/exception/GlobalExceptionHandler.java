package com.example.caching.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleEntityNotFoundException(EntityNotFoundException ex) {
        return "Entity not found: " + ex.getMessage();
    }

    @ExceptionHandler(CacheException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleCacheException(CacheException ex) {
        return "Cache operation failed: " + ex.getMessage();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException ex) {
        return "Invalid input: " + ex.getMessage();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception ex) {
        return "An unexpected error occurred: " + ex.getMessage();
    }

}
