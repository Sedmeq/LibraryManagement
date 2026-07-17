package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.AuthorRequestDto;
import org.example.librarymanagement.dto.AuthorResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.exception.ResourceNotFoundException;
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
@DisplayName("AuthorService – Integration Testlər")
class AuthorServiceIntegrationTest {

    @Autowired
    private AuthorService authorService;

    // -------------------------------------------------------------------------
    // create + getById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create: author verilənlər bazasına yazılır, getById ilə oxunur")
    void createAndGetById() {
        AuthorRequestDto dto = new AuthorRequestDto("Hüseyn Cavid", "Romantik şair");

        AuthorResponseDto created = authorService.create(dto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getFullName()).isEqualTo("Hüseyn Cavid");
        assertThat(created.getBiography()).isEqualTo("Romantik şair");

        AuthorResponseDto found = authorService.getById(created.getId());
        assertThat(found.getFullName()).isEqualTo("Hüseyn Cavid");
    }

    // -------------------------------------------------------------------------
    // getAll
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAll: birdən çox author siyahıda görünür")
    void getAll_multiplAuthors() {
        authorService.create(new AuthorRequestDto("Cavid", null));
        authorService.create(new AuthorRequestDto("Sabir", null));

        PageResponseDto<AuthorResponseDto> page = authorService.getAll(PageRequest.of(0, 10));

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update: author məlumatları dəyişdirilir")
    void update_changesData() {
        AuthorResponseDto created = authorService.create(new AuthorRequestDto("Köhnə Ad", "Köhnə bio"));

        AuthorResponseDto updated = authorService.update(
                created.getId(),
                new AuthorRequestDto("Yeni Ad", "Yeni bio")
        );

        assertThat(updated.getFullName()).isEqualTo("Yeni Ad");
        assertThat(updated.getBiography()).isEqualTo("Yeni bio");

        // DB-dən oxuyaraq yoxla
        AuthorResponseDto fromDb = authorService.getById(created.getId());
        assertThat(fromDb.getFullName()).isEqualTo("Yeni Ad");
    }

    @Test
    @DisplayName("update: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void update_notFound() {
        assertThatThrownBy(() ->
                authorService.update(999L, new AuthorRequestDto("X", "Y"))
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: author silinir, sonra getById exception atır")
    void delete_thenGetByIdThrows() {
        AuthorResponseDto created = authorService.create(new AuthorRequestDto("Silinəcək", null));
        Long id = created.getId();

        authorService.delete(id);

        assertThatThrownBy(() -> authorService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void delete_notFound() {
        assertThatThrownBy(() -> authorService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
