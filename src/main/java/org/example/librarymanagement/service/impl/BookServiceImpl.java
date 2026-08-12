package org.example.librarymanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.dto.BookResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.Author;
import org.example.librarymanagement.entity.Book;
import org.example.librarymanagement.entity.Category;
import org.example.librarymanagement.exception.DuplicateResourceException;
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

import java.util.Set;
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
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<BookResponseDto> getAll(Pageable pageable) {
        // findAll(Specification, Pageable) @EntityGraph(author) daşıyır -> N+1 aradan qaldırılıb.
        Page<BookResponseDto> page = bookRepository.findAll((Specification<Book>) null, pageable)
                .map(this::toResponse);
        return PageResponseDto.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<BookResponseDto> search(String title, String isbn, String authorName,
                                                   Integer yearFrom, Integer yearTo, Boolean onlyAvailable,
                                                   String categoryName,
                                                   Pageable pageable) {
        Specification<Book> spec = Specification
                .where(BookSpecification.titleContains(title))
                .and(BookSpecification.isbnEquals(isbn))
                .and(BookSpecification.authorNameContains(authorName))
                .and(BookSpecification.publicationYearFrom(yearFrom))
                .and(BookSpecification.publicationYearTo(yearTo))
                .and(BookSpecification.onlyAvailable(onlyAvailable))
                .and(BookSpecification.categoryNameEquals(categoryName));

        Page<BookResponseDto> page = bookRepository.findAll(spec, pageable).map(this::toResponse);
        return PageResponseDto.from(page);
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
