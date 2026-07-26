package org.example.librarymanagement.exception;

public class MaxActiveLoansExceededException extends RuntimeException {
    public MaxActiveLoansExceededException(String message) {
        super(message);
    }
}