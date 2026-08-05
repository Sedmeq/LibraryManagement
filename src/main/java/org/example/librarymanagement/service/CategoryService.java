package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.CategoryRequestDto;
import org.example.librarymanagement.dto.CategoryResponseDto;

import java.util.List;

public interface CategoryService {
    CategoryResponseDto create(CategoryRequestDto dto);
    CategoryResponseDto getById(Long id);
    List<CategoryResponseDto> getAll();
    CategoryResponseDto update(Long id, CategoryRequestDto dto);
    void delete(Long id);

    /** Kitaba category əlavə et (Many-to-Many) */
    BookCategoryResult addCategoryToBook(Long bookId, Long categoryId);

    /** Kitabdan category çıxar (Many-to-Many) */
    BookCategoryResult removeCategoryFromBook(Long bookId, Long categoryId);

    /** Kitabın bütün category-lərini döndər */
    List<CategoryResponseDto> getCategoriesByBook(Long bookId);

    record BookCategoryResult(Long bookId, String bookTitle, List<String> categoryNames) {}
}
