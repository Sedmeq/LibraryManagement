package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.AuthorRequestDto;
import org.example.librarymanagement.dto.AuthorResponseDto;
import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.dto.BookResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("BookService – Integration Testlər")
class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private AuthorService authorService;

    private Long authorId;
    private Long author2Id;

    @BeforeEach
    void setUp() {
        authorId = authorService.create(new AuthorRequestDto("Füzuli", "Klassik şair")).getId();
        author2Id = authorService.create(new AuthorRequestDto("Sabir", "Satirik şair")).getId();
    }

    // -------------------------------------------------------------------------
    // create + getById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create: kitab verilənlər bazasına yazılır, getById ilə oxunur")
    void createAndGetById() {
        BookRequestDto dto = new BookRequestDto("Leyli və Məcnun", "978-9952-20-001-1", 1600, authorId);

        BookResponseDto created = bookService.create(dto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Leyli və Məcnun");
        assertThat(created.getIsbn()).isEqualTo("978-9952-20-001-1");
        assertThat(created.getPublicationYear()).isEqualTo(1600);
        assertThat(created.getAuthorId()).isEqualTo(authorId);
        assertThat(created.getAuthorName()).isEqualTo("Füzuli");

        BookResponseDto found = bookService.getById(created.getId());
        assertThat(found.getTitle()).isEqualTo("Leyli və Məcnun");
    }

    @Test
    @DisplayName("create: mövcud olmayan authorId ilə ResourceNotFoundException atılır")
    void create_authorNotFound() {
        BookRequestDto dto = new BookRequestDto("X", "1234567890", 2000, 9999L);

        assertThatThrownBy(() -> bookService.create(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author");
    }

    // -------------------------------------------------------------------------
    // getAll
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAll: bir neçə kitab siyahıda görünür")
    void getAll_multipleBooks() {
        bookService.create(new BookRequestDto("Kitab 1", "1234567890", 2000, authorId));
        bookService.create(new BookRequestDto("Kitab 2", "0987654321", 2001, authorId));

        PageResponseDto<BookResponseDto> page = bookService.getAll(PageRequest.of(0, 10));

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // update – eyni author
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update: eyni author ilə kitab məlumatları dəyişdirilir")
    void update_sameAuthor() {
        BookResponseDto created = bookService.create(
                new BookRequestDto("Köhnə Başlıq", "1111111111", 2000, authorId));

        BookResponseDto updated = bookService.update(
                created.getId(),
                new BookRequestDto("Yeni Başlıq", "2222222222", 2005, authorId)
        );

        assertThat(updated.getTitle()).isEqualTo("Yeni Başlıq");
        assertThat(updated.getIsbn()).isEqualTo("2222222222");
        assertThat(updated.getPublicationYear()).isEqualTo(2005);
        assertThat(updated.getAuthorId()).isEqualTo(authorId);
    }

    // -------------------------------------------------------------------------
    // update – fərqli author
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update: fərqli author ilə kitabın autoru dəyişdirilir")
    void update_differentAuthor() {
        BookResponseDto created = bookService.create(
                new BookRequestDto("Şeir", "3333333333", 1900, authorId));

        BookResponseDto updated = bookService.update(
                created.getId(),
                new BookRequestDto("Şeir", "3333333333", 1900, author2Id)
        );

        assertThat(updated.getAuthorId()).isEqualTo(author2Id);
        assertThat(updated.getAuthorName()).isEqualTo("Sabir");
    }

    @Test
    @DisplayName("update: mövcud olmayan kitab id ilə ResourceNotFoundException atılır")
    void update_bookNotFound() {
        assertThatThrownBy(() ->
                bookService.update(9999L, new BookRequestDto("X", "1234567890", 2000, authorId))
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book");
    }

    @Test
    @DisplayName("update: mövcud olmayan yeni author id ilə ResourceNotFoundException atılır")
    void update_newAuthorNotFound() {
        BookResponseDto created = bookService.create(
                new BookRequestDto("Şeir", "4444444444", 1900, authorId));

        assertThatThrownBy(() ->
                bookService.update(created.getId(),
                        new BookRequestDto("Şeir", "4444444444", 1900, 9999L))
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author");
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: kitab silinir, sonra getById exception atır")
    void delete_thenGetByIdThrows() {
        BookResponseDto created = bookService.create(
                new BookRequestDto("Silinəcək", "5555555555", 2000, authorId));
        Long id = created.getId();

        bookService.delete(id);

        assertThatThrownBy(() -> bookService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void delete_notFound() {
        assertThatThrownBy(() -> bookService.delete(9999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
