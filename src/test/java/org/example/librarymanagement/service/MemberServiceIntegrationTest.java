package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.MemberRequestDto;
import org.example.librarymanagement.dto.MemberResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.exception.DuplicateResourceException;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("MemberService – Integration Testlər")
class MemberServiceIntegrationTest {

    @Autowired
    private MemberService memberService;

    // -------------------------------------------------------------------------
    // create + getById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create: member verilənlər bazasına yazılır, getById ilə oxunur")
    void createAndGetById() {
        MemberRequestDto dto = new MemberRequestDto(
                "Leyla Hüseynova",
                "leyla@example.com",
                LocalDate.of(2024, 3, 1)
        );

        MemberResponseDto created = memberService.create(dto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getFullName()).isEqualTo("Leyla Hüseynova");
        assertThat(created.getEmail()).isEqualTo("leyla@example.com");
        assertThat(created.getMembershipDate()).isEqualTo(LocalDate.of(2024, 3, 1));

        MemberResponseDto found = memberService.getById(created.getId());
        assertThat(found.getEmail()).isEqualTo("leyla@example.com");
    }

    @Test
    @DisplayName("create: mövcud email ilə DuplicateResourceException atılır")
    void create_duplicateEmail() {
        MemberRequestDto dto = new MemberRequestDto(
                "Birinci İstifadəçi",
                "same@example.com",
                LocalDate.of(2023, 1, 1)
        );
        memberService.create(dto);

        MemberRequestDto duplicate = new MemberRequestDto(
                "İkinci İstifadəçi",
                "same@example.com",
                LocalDate.of(2023, 6, 1)
        );

        assertThatThrownBy(() -> memberService.create(duplicate))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("same@example.com");
    }

    // -------------------------------------------------------------------------
    // getAll
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAll: bir neçə member siyahıda görünür")
    void getAll_multipleMembers() {
        memberService.create(new MemberRequestDto("Üzv 1", "uzv1@example.com", LocalDate.now()));
        memberService.create(new MemberRequestDto("Üzv 2", "uzv2@example.com", LocalDate.now()));

        PageResponseDto<MemberResponseDto> page = memberService.getAll(PageRequest.of(0, 10));

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("getAll: pagination düzgün işləyir")
    void getAll_pagination() {
        for (int i = 1; i <= 5; i++) {
            memberService.create(new MemberRequestDto(
                    "Üzv " + i,
                    "uzv_page" + i + "@example.com",
                    LocalDate.now()
            ));
        }

        PageResponseDto<MemberResponseDto> page = memberService.getAll(PageRequest.of(0, 3));

        assertThat(page.getContent().size()).isEqualTo(3);
        assertThat(page.getPageSize()).isEqualTo(3);
        assertThat(page.isLast()).isFalse();
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update: member məlumatları dəyişdirilir")
    void update_changesData() {
        MemberResponseDto created = memberService.create(new MemberRequestDto(
                "Köhnə Ad",
                "kohne@example.com",
                LocalDate.of(2022, 1, 1)
        ));

        MemberResponseDto updated = memberService.update(
                created.getId(),
                new MemberRequestDto("Yeni Ad", "yeni@example.com", LocalDate.of(2023, 6, 15))
        );

        assertThat(updated.getFullName()).isEqualTo("Yeni Ad");
        assertThat(updated.getEmail()).isEqualTo("yeni@example.com");
        assertThat(updated.getMembershipDate()).isEqualTo(LocalDate.of(2023, 6, 15));

        // DB-dən oxuyaraq yoxla
        MemberResponseDto fromDb = memberService.getById(created.getId());
        assertThat(fromDb.getFullName()).isEqualTo("Yeni Ad");
    }

    @Test
    @DisplayName("update: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void update_notFound() {
        assertThatThrownBy(() ->
                memberService.update(9999L, new MemberRequestDto("X", "x@x.com", LocalDate.now()))
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member");
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: member silinir, sonra getById exception atır")
    void delete_thenGetByIdThrows() {
        MemberResponseDto created = memberService.create(new MemberRequestDto(
                "Silinəcək",
                "sil@example.com",
                LocalDate.now()
        ));
        Long id = created.getId();

        memberService.delete(id);

        assertThatThrownBy(() -> memberService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void delete_notFound() {
        assertThatThrownBy(() -> memberService.delete(9999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
