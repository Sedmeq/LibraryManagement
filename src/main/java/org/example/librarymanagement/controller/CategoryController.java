package org.example.librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.CategoryRequestDto;
import org.example.librarymanagement.dto.CategoryResponseDto;
import org.example.librarymanagement.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Kateqoriya CRUD və Book↔Category many-to-many əməliyyatları")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Yeni kateqoriya yarat")
    public ResponseEntity<CategoryResponseDto> create(@Valid @RequestBody CategoryRequestDto dto,
                                                      UriComponentsBuilder uriBuilder) {
        CategoryResponseDto created = categoryService.create(dto);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/categories/{id}").buildAndExpand(created.getId()).toUri())
                .body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Id ilə kateqoriya al")
    public ResponseEntity<CategoryResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Bütün kateqoriyaların siyahısı")
    public ResponseEntity<List<CategoryResponseDto>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kateqoriya adını yenilə")
    public ResponseEntity<CategoryResponseDto> update(@PathVariable Long id,
                                                      @Valid @RequestBody CategoryRequestDto dto) {
        return ResponseEntity.ok(categoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kateqoriya sil")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Book ↔ Category many-to-many əməliyyatları
    // -------------------------------------------------------------------------

    @PostMapping("/{categoryId}/books/{bookId}")
    @Operation(summary = "Kitaba kateqoriya əlavə et (Many-to-Many)")
    public ResponseEntity<CategoryService.BookCategoryResult> addToBook(
            @PathVariable Long categoryId,
            @PathVariable Long bookId) {
        return ResponseEntity.ok(categoryService.addCategoryToBook(bookId, categoryId));
    }

    @DeleteMapping("/{categoryId}/books/{bookId}")
    @Operation(summary = "Kitabdan kateqoriya çıxar (Many-to-Many)")
    public ResponseEntity<CategoryService.BookCategoryResult> removeFromBook(
            @PathVariable Long categoryId,
            @PathVariable Long bookId) {
        return ResponseEntity.ok(categoryService.removeCategoryFromBook(bookId, categoryId));
    }

    @GetMapping("/by-book/{bookId}")
    @Operation(summary = "Kitabın bütün kateqoriyalarını al")
    public ResponseEntity<List<CategoryResponseDto>> getCategoriesByBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(categoryService.getCategoriesByBook(bookId));
    }
}
