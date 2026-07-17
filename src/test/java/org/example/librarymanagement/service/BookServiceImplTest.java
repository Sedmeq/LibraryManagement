package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.dto.BookResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.Author;
import org.example.librarymanagement.entity.Book;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.AuthorRepository;
import org.example.librarymanagement.repository.BookRepository;
import org.example.librarymanagement.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookServiceImpl – Unit Testlər")
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Author author;
    private Book book;
    private BookRequestDto requestDto;

    @BeforeEach
    void setUp() {
        author = Author.builder()
                .id(1L)
                .fullName("Əliağa Vahid")
                .biography("Azərbaycan şairi")
                .books(new ArrayList<>())
                .build();

        book = Book.builder()
                .id(10L)
                .title("Divani")
                .isbn("978-9952-20-001-1")
                .publicationYear(1940)
                .author(author)
                .build();

        requestDto = new BookRequestDto("Divani", "978-9952-20-001-1", 1940, 1L);
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create: mövcud author ilə kitab yaradılır")
    void create_success() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        BookResponseDto result = bookService.create(requestDto);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("Divani");
        assertThat(result.getIsbn()).isEqualTo("978-9952-20-001-1");
        assertThat(result.getPublicationYear()).isEqualTo(1940);
        assertThat(result.getAuthorId()).isEqualTo(1L);
        assertThat(result.getAuthorName()).isEqualTo("Əliağa Vahid");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("create: mövcud olmayan authorId ilə ResourceNotFoundException atılır")
    void create_authorNotFound_throwsException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());
        BookRequestDto dto = new BookRequestDto("Test", "1234567890", 2000, 99L);

        assertThatThrownBy(() -> bookService.create(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author")
                .hasMessageContaining("99");

        verify(bookRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getById()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getById: mövcud id ilə kitab qaytarılır")
    void getById_found() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        BookResponseDto result = bookService.getById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("Divani");
        verify(bookRepository).findById(10L);
    }

    @Test
    @DisplayName("getById: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void getById_notFound_throwsException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book")
                .hasMessageContaining("99");
    }

    // -------------------------------------------------------------------------
    // getAll()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAll: siyahı düzgün PageResponseDto ilə qaytarılır")
    void getAll_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Book> bookPage = new PageImpl<>(List.of(book), pageable, 1);
        when(bookRepository.findAll(pageable)).thenReturn(bookPage);

        PageResponseDto<BookResponseDto> result = bookService.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Divani");
    }

    @Test
    @DisplayName("getAll: boş siyahı qaytarılır")
    void getAll_emptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponseDto<BookResponseDto> result = bookService.getAll(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // update() – eyni author
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update: eyni author ilə kitab yenilənir")
    void update_sameAuthor_success() {
        BookRequestDto updateDto = new BookRequestDto("Yeni Başlıq", "978-0-00-000001-1", 2000, 1L);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookResponseDto result = bookService.update(10L, updateDto);

        assertThat(result.getTitle()).isEqualTo("Yeni Başlıq");
        assertThat(result.getIsbn()).isEqualTo("978-0-00-000001-1");
        assertThat(result.getPublicationYear()).isEqualTo(2000);
        // Eyni author – authorRepository.findById çağrılmamalıdır
        verify(authorRepository, never()).findById(any());
    }

    @Test
    @DisplayName("update: fərqli author ilə kitab yenilənir")
    void update_differentAuthor_success() {
        Author newAuthor = Author.builder()
                .id(2L)
                .fullName("Sabir")
                .biography("Satirik şair")
                .books(new ArrayList<>())
                .build();
        BookRequestDto updateDto = new BookRequestDto("Yeni Başlıq", "978-0-00-000001-1", 2000, 2L);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(authorRepository.findById(2L)).thenReturn(Optional.of(newAuthor));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookResponseDto result = bookService.update(10L, updateDto);

        assertThat(result.getAuthorId()).isEqualTo(2L);
        assertThat(result.getAuthorName()).isEqualTo("Sabir");
        verify(authorRepository).findById(2L);
    }

    @Test
    @DisplayName("update: kitab tapılmadıqda ResourceNotFoundException atılır")
    void update_bookNotFound_throwsException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(99L, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book")
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update: yeni author tapılmadıqda ResourceNotFoundException atılır")
    void update_newAuthorNotFound_throwsException() {
        BookRequestDto updateDto = new BookRequestDto("X", "1234567890", 2000, 77L);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(authorRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(10L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author")
                .hasMessageContaining("77");
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: mövcud kitab silinir")
    void delete_success() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        doNothing().when(bookRepository).delete(book);

        bookService.delete(10L);

        verify(bookRepository).delete(book);
    }

    @Test
    @DisplayName("delete: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void delete_notFound_throwsException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(bookRepository, never()).delete(any());
    }
}
