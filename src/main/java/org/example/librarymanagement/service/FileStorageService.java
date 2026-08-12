package org.example.librarymanagement.service;

public interface FileStorageService {
    String store(Long bookId, org.springframework.web.multipart.MultipartFile file);
    org.springframework.core.io.Resource loadAsResource(Long bookId);
}