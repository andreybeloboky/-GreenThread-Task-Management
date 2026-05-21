package org.example.exception;

public class SuccessResponse extends RuntimeException {
    public SuccessResponse(String message) {
        super(message);
    }
}
