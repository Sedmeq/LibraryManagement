package org.example.librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/books/{bookId}/cover")
@RequiredArgsConstructor
@Tag(name = "Book Files", description = "Kitab cover şəklinin upload/download əməliyyatları")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Kitaba cover şəkli yüklə (jpg/jpeg/png, max 5MB)")
    public ResponseEntity<String> upload(@PathVariable Long bookId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok("Fayl uğurla yükləndi: " + fileStorageService.store(bookId, file));
    }

    @GetMapping
    @Operation(summary = "Kitabın cover şəklini endir")
    public ResponseEntity<Resource> download(@PathVariable Long bookId) {
        Resource resource = fileStorageService.loadAsResource(bookId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}