package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.MemberRequestDto;
import org.example.librarymanagement.dto.MemberResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.Member;
import org.example.librarymanagement.exception.DuplicateResourceException;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.MemberRepository;
import org.example.librarymanagement.service.impl.MemberServiceImpl;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberServiceImpl – Unit Testlər")
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member member;
    private MemberRequestDto requestDto;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .fullName("Aysel Məmmədova")
                .email("aysel@example.com")
                .membershipDate(LocalDate.of(2023, 5, 10))
                .build();

        requestDto = new MemberRequestDto(
                "Aysel Məmmədova",
                "aysel@example.com",
                LocalDate.of(2023, 5, 10)
        );
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create: unikal email ilə member yaradılır")
    void create_success() {
        when(memberRepository.findByEmail("aysel@example.com")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        MemberResponseDto result = memberService.create(requestDto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFullName()).isEqualTo("Aysel Məmmədova");
        assertThat(result.getEmail()).isEqualTo("aysel@example.com");
        assertThat(result.getMembershipDate()).isEqualTo(LocalDate.of(2023, 5, 10));
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("create: mövcud email ilə DuplicateResourceException atılır")
    void create_duplicateEmail_throwsException() {
        when(memberRepository.findByEmail("aysel@example.com")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.create(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("aysel@example.com");

        verify(memberRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getById()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getById: mövcud id ilə member qaytarılır")
    void getById_found() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponseDto result = memberService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("aysel@example.com");
        verify(memberRepository).findById(1L);
    }

    @Test
    @DisplayName("getById: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void getById_notFound_throwsException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member")
                .hasMessageContaining("99");
    }

    // -------------------------------------------------------------------------
    // getAll()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAll: siyahı düzgün PageResponseDto ilə qaytarılır")
    void getAll_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Member> memberPage = new PageImpl<>(List.of(member), pageable, 1);
        when(memberRepository.findAll(pageable)).thenReturn(memberPage);

        PageResponseDto<MemberResponseDto> result = memberService.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("getAll: boş siyahı qaytarılır")
    void getAll_emptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(memberRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponseDto<MemberResponseDto> result = memberService.getAll(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update: mövcud member yenilənir")
    void update_success() {
        MemberRequestDto updateDto = new MemberRequestDto(
                "Yeni Ad",
                "yeni@example.com",
                LocalDate.of(2024, 1, 1)
        );
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberResponseDto result = memberService.update(1L, updateDto);

        assertThat(result.getFullName()).isEqualTo("Yeni Ad");
        assertThat(result.getEmail()).isEqualTo("yeni@example.com");
        assertThat(result.getMembershipDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("update: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void update_notFound_throwsException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.update(99L, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: mövcud member silinir")
    void delete_success() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        doNothing().when(memberRepository).delete(member);

        memberService.delete(1L);

        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("delete: mövcud olmayan id ilə ResourceNotFoundException atılır")
    void delete_notFound_throwsException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(memberRepository, never()).delete(any());
    }
}
