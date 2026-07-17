package org.example.librarymanagement.exception;

// Unikal sahə (məs. email, isbn) təkrarlandıqda 409 Conflict üçün
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
