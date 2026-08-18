package org.example.librarymanagement.exception;

/**
 * Thrown when search/filter parameters are logically inconsistent —
 * e.g. yearFrom > yearTo or loanDateFrom > loanDateTo.
 */
public class InvalidSearchException extends RuntimeException {
    public InvalidSearchException(String message) {
        super(message);
    }
}
