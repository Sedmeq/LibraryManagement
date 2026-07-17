package org.example.librarymanagement.service;


import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.dto.BookResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.springframework.data.domain.Pageable;

public interface BookService {
    BookResponseDto create(BookRequestDto dto);
    BookResponseDto getById(Long id);
    PageResponseDto<BookResponseDto> getAll(Pageable pageable);
    BookResponseDto update(Long id, BookRequestDto dto);
    void delete(Long id);
}
