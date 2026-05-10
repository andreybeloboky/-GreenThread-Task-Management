package org.example.exception;

public class ErrorResponse extends RuntimeException {
    public ErrorResponse(String message) {
        super(message);
    }
}
