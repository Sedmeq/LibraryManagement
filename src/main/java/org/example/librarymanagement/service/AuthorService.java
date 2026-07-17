package org.example.librarymanagement.service;


import org.example.librarymanagement.dto.AuthorRequestDto;
import org.example.librarymanagement.dto.AuthorResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.springframework.data.domain.Pageable;

public interface AuthorService {
    AuthorResponseDto create(AuthorRequestDto dto);
    AuthorResponseDto getById(Long id);
    PageResponseDto<AuthorResponseDto> getAll(Pageable pageable);
    AuthorResponseDto update(Long id, AuthorRequestDto dto);
    void delete(Long id);
}
