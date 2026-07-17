package org.example.librarymanagement.exception;

// 404 Not Found üçün custom exception
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Long id) {
        return new ResourceNotFoundException(entity + " tapılmadı, id = " + id);
    }
}
