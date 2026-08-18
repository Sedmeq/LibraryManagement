package org.example.librarymanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.dto.BookResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.Author;
import org.example.librarymanagement.entity.Book;
import org.example.librarymanagement.entity.Category;
import org.example.librarymanagement.exception.DuplicateResourceException;
import org.example.librarymanagement.exception.InvalidSearchException;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.AuthorRepository;
import org.example.librarymanagement.repository.BookRepository;
import org.example.librarymanagement.service.BookService;
import org.example.librarymanagement.specification.BookSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Override
    public BookResponseDto create(BookRequestDto dto) {
        Author author = authorRepository.findById(dto.getAuthorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Author", dto.getAuthorId()));

        bookRepository.findByIsbn(dto.getIsbn()).ifPresent(existing -> {
            throw new DuplicateResourceException("Bu ISBN artıq istifadə olunur: " + dto.getIsbn());
        });

        Book book = Book.builder()
                .title(dto.getTitle())
                .isbn(dto.getIsbn())
                .publicationYear(dto.getPublicationYear())
                .author(author)
                .totalCopies(dto.getTotalCopies())
                .availableCopies(dto.getTotalCopies())
                .build();

        return toResponse(bookRepository.save(book));
    }

    @Override
    @Cacheable(cacheNames = "books", key = "#id")
    @Transactional(readOnly = true)
    public BookResponseDto getById(Long id) {
        // findByIdWithCategories uses JOIN FETCH — safe for single entity, no pagination involved
        Book book = bookRepository.findByIdWithCategories(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", id));
        return toResponse(book);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<BookResponseDto> getAll(Pageable pageable) {
        Page<Book> page = bookRepository.findAll((Specification<Book>) null, pageable);
        return toPageResponseWithCategories(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<BookResponseDto> search(String title, String isbn, String authorName,
                                                   Integer yearFrom, Integer yearTo, Boolean onlyAvailable,
                                                   String categoryName,
                                                   Pageable pageable) {
        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            throw new InvalidSearchException(
                    "yearFrom (" + yearFrom + ") yearTo-dan (" + yearTo + ") böyük ola bilməz.");
        }

        Specification<Book> spec = Specification
                .where(BookSpecification.titleContains(title))
                .and(BookSpecification.isbnEquals(isbn))
                .and(BookSpecification.authorNameContains(authorName))
                .and(BookSpecification.publicationYearFrom(yearFrom))
                .and(BookSpecification.publicationYearTo(yearTo))
                .and(BookSpecification.onlyAvailable(onlyAvailable))
                .and(BookSpecification.categoryNameEquals(categoryName));

        Page<Book> page = bookRepository.findAll(spec, pageable);
        return toPageResponseWithCategories(page);
    }

    /**
     * Updates the book and immediately refreshes the cache entry with the new data.
     * Using @CachePut instead of @CacheEvict means the next getById call is served
     * from cache without an extra DB round-trip.
     */
    @Override
    @CachePut(cacheNames = "books", key = "#id")
    public BookResponseDto update(Long id, BookRequestDto dto) {
        Book book = findEntity(id);

        if (!book.getIsbn().equals(dto.getIsbn())) {
            bookRepository.findByIsbn(dto.getIsbn()).ifPresent(existing -> {
                throw new DuplicateResourceException("Bu ISBN artıq istifadə olunur: " + dto.getIsbn());
            });
        }

        if (!book.getAuthor().getId().equals(dto.getAuthorId())) {
            Author newAuthor = authorRepository.findById(dto.getAuthorId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Author", dto.getAuthorId()));
            book.setAuthor(newAuthor);
        }

        int borrowedCopies = book.getTotalCopies() - book.getAvailableCopies();
        book.setTitle(dto.getTitle());
        book.setIsbn(dto.getIsbn());
        book.setPublicationYear(dto.getPublicationYear());
        book.setTotalCopies(dto.getTotalCopies());
        book.setAvailableCopies(Math.max(0, dto.getTotalCopies() - borrowedCopies));

        return toResponse(bookRepository.save(book));
    }

    @Override
    @CacheEvict(cacheNames = "books", key = "#id")
    public void delete(Long id) {
        Book book = findEntity(id);
        bookRepository.delete(book);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Two-query strategy to avoid Hibernate in-memory pagination warning (HHH90003004).
     *
     * Problem: fetching a collection (categories) with JOIN in a paginated query causes
     * Hibernate to load ALL rows into memory and paginate in Java, not in SQL.
     *
     * Solution:
     *   Query 1 — paginated fetch with only @EntityGraph("author") — DB-level LIMIT works correctly.
     *   Query 2 — load categories for exactly those book ids via a separate LEFT JOIN FETCH.
     *   Then merge the categories back onto the already-fetched books.
     *
     * Result: correct DB-level pagination + no N+1 on categories.
     */
    private PageResponseDto<BookResponseDto> toPageResponseWithCategories(Page<Book> page) {
        if (page.isEmpty()) {
            return PageResponseDto.from(page.map(this::toResponse));
        }

        List<Long> ids = page.getContent().stream().map(Book::getId).toList();

        // Single query fetches categories for all ids at once
        Map<Long, Set<Category>> categoryMap = bookRepository.findWithCategoriesByIds(ids)
                .stream()
                .collect(Collectors.toMap(Book::getId, Book::getCategories));

        return PageResponseDto.from(page.map(book -> {
            Set<Category> cats = categoryMap.getOrDefault(book.getId(), Set.of());
            book.getCategories().clear();
            book.getCategories().addAll(cats);
            return toResponse(book);
        }));
    }

    private Book findEntity(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", id));
    }

    private BookResponseDto toResponse(Book book) {
        Set<String> categoryNames = book.getCategories() == null ? Set.of() :
                book.getCategories().stream()
                        .map(Category::getName)
                        .collect(Collectors.toSet());

        return BookResponseDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .publicationYear(book.getPublicationYear())
                .authorId(book.getAuthor().getId())
                .authorName(book.getAuthor().getFullName())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .categoryNames(categoryNames)
                .build();
    }
}
