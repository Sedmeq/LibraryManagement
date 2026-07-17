package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.AuthorRequestDto;
import org.example.librarymanagement.dto.AuthorResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.Author;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.AuthorRepository;
import org.example.librarymanagement.service.impl.AuthorServiceImpl;
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
@DisplayName("AuthorServiceImpl – Unit Testlər")
class AuthorServiceImplTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorServiceImpl authorService;

    private Author author;
    private AuthorRequestDto requestDto;

    @BeforeEach
    void setUp() {
        author = Author.builder()
                .id(1L)
                .fullName("Əliağa Vahid")
                .biography("Azərbaycan şairi")
                .books(new ArrayList<>())
                .build();

        requestDto = new AuthorRequestDto("Əliağa Vahid", "Azərbaycan şairi");
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create: düzgün məlumat ilə author yaradılır")
    void create_success() {
        when(authorRepository.save(any(Author.class))).thenReturn(author);

        AuthorResponseDto result = authorService.create(requestDto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFullName()).isEqualTo("Əliağa Vahid");
        assertThat(result.getBiography()).isEqualTo("Azərbaycan şairi");
        assertThat(result.getBookTitles()).isEmpty();
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    @DisplayName("create: null books siyahısı ilə author yaradılır")
    void create_nullBooks_returnsEmptyList() {
        Author noBooks = Author.builder()
                .id(2L)
                .fullName("Füzuli")
                .biography("Klassik şair")
                .books(null)
                .build();
        when(authorRepository.save(any(Author.class))).thenReturn(noBooks);

        AuthorResponseDto result = authorService.create(new AuthorRequestDto("Füzuli", "Klassik şair"));

        assertThat(result.getBookTitles()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getById()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getById: mövcud id ilə author qaytarılır")
    void getById_found() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        AuthorResponseDto result = authorService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFullName()).isEqualTo("Əliağa Vahid");
        verify(authorRepository).findById(1L);
    }

    @Test
    @DisplayName("getById: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void getById_notFound_throwsException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author")
                .hasMessageContaining("99");
    }

    // -------------------------------------------------------------------------
    // getAll()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAll: siyahı düzgün PageResponseDto ilə qaytarılır")
    void getAll_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Author> authorPage = new PageImpl<>(List.of(author), pageable, 1);
        when(authorRepository.findAll(pageable)).thenReturn(authorPage);

        PageResponseDto<AuthorResponseDto> result = authorService.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("getAll: boş siyahı qaytarılır")
    void getAll_emptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Author> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(authorRepository.findAll(pageable)).thenReturn(emptyPage);

        PageResponseDto<AuthorResponseDto> result = authorService.getAll(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update: mövcud author yenilənir")
    void update_success() {
        AuthorRequestDto updateDto = new AuthorRequestDto("Yenilənmiş Ad", "Yeni bioqrafiya");
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthorResponseDto result = authorService.update(1L, updateDto);

        assertThat(result.getFullName()).isEqualTo("Yenilənmiş Ad");
        assertThat(result.getBiography()).isEqualTo("Yeni bioqrafiya");
        verify(authorRepository).save(author);
    }

    @Test
    @DisplayName("update: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void update_notFound_throwsException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.update(99L, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: mövcud author silinir")
    void delete_success() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        doNothing().when(authorRepository).delete(author);

        authorService.delete(1L);

        verify(authorRepository).delete(author);
    }

    @Test
    @DisplayName("delete: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void delete_notFound_throwsException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(authorRepository, never()).delete(any());
    }
}
