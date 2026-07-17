package org.example.librarymanagement.service;


import org.example.librarymanagement.dto.MemberRequestDto;
import org.example.librarymanagement.dto.MemberResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.springframework.data.domain.Pageable;

public interface MemberService {
    MemberResponseDto create(MemberRequestDto dto);
    MemberResponseDto getById(Long id);
    PageResponseDto<MemberResponseDto> getAll(Pageable pageable);
    MemberResponseDto update(Long id, MemberRequestDto dto);
    void delete(Long id);
}
