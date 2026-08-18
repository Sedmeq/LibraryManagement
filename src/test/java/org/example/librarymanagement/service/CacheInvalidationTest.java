package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.dto.BookResponseDto;
import org.example.librarymanagement.entity.Author;
import org.example.librarymanagement.entity.Book;
import org.example.librarymanagement.repository.AuthorRepository;
import org.example.librarymanagement.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SpringBootTest
@ActiveProfiles("test")
class CacheInvalidationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private AuthorRepository authorRepository;

    private Author author;
    private Book book;

    @BeforeEach
    void setUp() {
        // Clear cache before each test to ensure isolation
        Cache booksCache = cacheManager.getCache("books");
        if (booksCache != null) booksCache.clear();

        author = Author.builder()
                .id(1L)
                .fullName("Test Author")
                .build();

        book = Book.builder()
                .id(1L)
                .title("Old Title")
                .isbn("978-0-00-000001-1")
                .publicationYear(2000)
                .author(author)
                .totalCopies(3)
                .availableCopies(3)
                .build();
    }

    @Test
    @DisplayName("@Cacheable — ikinci getById DB-yə getmir, cache-dən gəlir")
    void getById_secondCall_servedFromCache() {
        when(bookRepository.findByIdWithCategories(1L)).thenReturn(Optional.of(book));

        bookService.getById(1L); // cache miss → DB
        bookService.getById(1L); // cache hit → no DB call

        verify(bookRepository, times(1)).findByIdWithCategories(1L);
    }

    @Test
    @DisplayName("@CachePut — update sonrası cache yenilənir, köhnə data gəlmir")
    void update_cacheRefreshedWithNewData() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.findByIsbn(any())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setTitle("New Title");
            return b;
        });

        BookRequestDto updateDto = new BookRequestDto(
                "New Title", "978-0-00-000001-1", 2000, 1L, 3);

        BookResponseDto updated = bookService.update(1L, updateDto);
        assertThat(updated.getTitle()).isEqualTo("New Title");

        // Cache should now hold the updated entry — no extra DB call needed
        Cache booksCache = cacheManager.getCache("books");
        assertThat(booksCache).isNotNull();
        BookResponseDto cached = booksCache.get(1L, BookResponseDto.class);
        assertThat(cached).isNotNull();
        assertThat(cached.getTitle()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("@CacheEvict — delete sonrası cache silinir, növbəti getById DB-yə gedir")
    void delete_evictsCache_nextGetByIdHitsDb() {
        when(bookRepository.findByIdWithCategories(1L)).thenReturn(Optional.of(book));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        doNothing().when(bookRepository).delete(book);

        bookService.getById(1L); // populate cache — findByIdWithCategories call #1
        bookService.delete(1L);  // @CacheEvict fires — findById call (inside findEntity)

        // After eviction the cache entry must be gone
        Cache booksCache = cacheManager.getCache("books");
        assertThat(booksCache).isNotNull();
        assertThat(booksCache.get(1L)).isNull();

        // Next call must go back to the repository — findByIdWithCategories call #2
        bookService.getById(1L);
        verify(bookRepository, times(2)).findByIdWithCategories(1L);
        verify(bookRepository, times(1)).findById(1L);
    }
}
