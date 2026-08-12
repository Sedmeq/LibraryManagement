package org.example.librarymanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.entity.Book;
import org.example.librarymanagement.exception.InvalidFileException;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.BookRepository;
import org.example.librarymanagement.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final BookRepository bookRepository;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.max-size-bytes}")
    private long maxSizeBytes;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    @Override
    public String store(Long bookId, MultipartFile file) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", bookId));

        if (file.isEmpty()) throw new InvalidFileException("Fayl boşdur");
        if (file.getSize() > maxSizeBytes) throw new InvalidFileException("Fayl ölçüsü max 5MB-ı aşır");

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension))
            throw new InvalidFileException("Yalnız jpg/jpeg/png fayllarına icazə verilir");
        if (!hasValidImageSignature(file))
            throw new InvalidFileException("Faylın həqiqi tipi uzantı ilə uyğun gəlmir");

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            String filename = "book-" + bookId + "." + extension;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            book.setCoverImagePath(target.toString());
            bookRepository.save(book);
            return filename;
        } catch (IOException e) {
            throw new InvalidFileException("Fayl yadda saxlanılarkən xəta: " + e.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", bookId));
        if (book.getCoverImagePath() == null)
            throw new ResourceNotFoundException("Bu kitab üçün cover şəkli tapılmadı");
        try {
            Path path = Paths.get(book.getCoverImagePath());
            UrlResource resource = new UrlResource(path.toUri());
            if (!resource.exists()) throw new ResourceNotFoundException("Cover fayl fiziki tapılmadı");
            return resource;
        } catch (MalformedURLException e) {
            throw new InvalidFileException("Fayl yolu yanlışdır");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains("."))
            throw new InvalidFileException("Fayl uzantısı müəyyən edilə bilmədi");
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean hasValidImageSignature(MultipartFile file) {
        try {
            byte[] h = file.getInputStream().readNBytes(8);
            boolean isJpeg = h.length >= 3 && h[0] == (byte) 0xFF && h[1] == (byte) 0xD8 && h[2] == (byte) 0xFF;
            boolean isPng = h.length >= 8 && h[0] == (byte) 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47;
            return isJpeg || isPng;
        } catch (IOException e) {
            return false;
        }
    }
}