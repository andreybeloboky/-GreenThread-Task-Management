package org.example.exception;

public class DataNotExistsException extends RuntimeException {
    public DataNotExistsException(String message) {
        super(message);
    }
}
