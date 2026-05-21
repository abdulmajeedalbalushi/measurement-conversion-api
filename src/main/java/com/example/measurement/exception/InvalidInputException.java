package com.example.measurement.exception;

/**
 * Thrown when the measurement string is syntactically invalid.
 * Mapped to HTTP 400 by the global exception handler.
 */
public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}
