package org.example.librarymanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.CategoryRequestDto;
import org.example.librarymanagement.dto.CategoryResponseDto;
import org.example.librarymanagement.entity.Book;
import org.example.librarymanagement.entity.Category;
import org.example.librarymanagement.exception.DuplicateResourceException;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.BookRepository;
import org.example.librarymanagement.repository.CategoryRepository;
import org.example.librarymanagement.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    @Override
    public CategoryResponseDto create(CategoryRequestDto dto) {
        categoryRepository.findByNameIgnoreCase(dto.getName()).ifPresent(c -> {
            throw new DuplicateResourceException(
                    "Bu adda category artıq mövcuddur: " + dto.getName());
        });
        Category saved = categoryRepository.save(
                Category.builder().name(dto.getName()).build());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponseDto update(Long id, CategoryRequestDto dto) {
        Category category = findEntity(id);

        // Eyni adda başqa category varsa conflict
        categoryRepository.findByNameIgnoreCase(dto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException(
                        "Bu adda category artıq mövcuddur: " + dto.getName());
            }
        });

        category.setName(dto.getName());
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(Long id) {
        Category category = findEntity(id);
        // Many-to-Many join table entries are managed by Book entity owner side;
        // removing from all books first to keep DB consistent
        for (Book book : category.getBooks()) {
            book.getCategories().remove(category);
            bookRepository.save(book);
        }
        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public BookCategoryResult addCategoryToBook(Long bookId, Long categoryId) {
        // findByIdWithCategories uses JOIN FETCH — N+1 aradan qaldırılıb
        Book book = bookRepository.findByIdWithCategories(bookId)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", bookId));
        Category category = findEntity(categoryId);

        book.getCategories().add(category);
        bookRepository.save(book);

        return toBookCategoryResult(book);
    }

    @Override
    @Transactional
    public BookCategoryResult removeCategoryFromBook(Long bookId, Long categoryId) {
        Book book = bookRepository.findByIdWithCategories(bookId)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", bookId));
        Category category = findEntity(categoryId);

        book.getCategories().remove(category);
        bookRepository.save(book);

        return toBookCategoryResult(book);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategoriesByBook(Long bookId) {
        Book book = bookRepository.findByIdWithCategories(bookId)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", bookId));
        return book.getCategories().stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------

    private Category findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    private CategoryResponseDto toResponse(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .bookCount(category.getBooks() == null ? 0 : category.getBooks().size())
                .build();
    }

    private BookCategoryResult toBookCategoryResult(Book book) {
        List<String> names = book.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        return new BookCategoryResult(book.getId(), book.getTitle(), names);
    }
}
