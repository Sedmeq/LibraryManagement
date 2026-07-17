package org.example.librarymanagement.service.impl;


import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.AuthorRequestDto;
import org.example.librarymanagement.dto.AuthorResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.Author;
import org.example.librarymanagement.entity.Book;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.AuthorRepository;
import org.example.librarymanagement.service.AuthorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    @Override
    public AuthorResponseDto create(AuthorRequestDto dto) {
        Author author = Author.builder()
                .fullName(dto.getFullName())
                .biography(dto.getBiography())
                .build();
        return toResponse(authorRepository.save(author));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorResponseDto getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<AuthorResponseDto> getAll(Pageable pageable) {
        Page<AuthorResponseDto> page = authorRepository.findAll(pageable).map(this::toResponse);
        return PageResponseDto.from(page);
    }

    @Override
    public AuthorResponseDto update(Long id, AuthorRequestDto dto) {
        Author author = findEntity(id);
        author.setFullName(dto.getFullName());
        author.setBiography(dto.getBiography());
        return toResponse(authorRepository.save(author));
    }

    @Override
    public void delete(Long id) {
        Author author = findEntity(id);
        authorRepository.delete(author);
    }

    private Author findEntity(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Author", id));
    }

    // Entity -> DTO mapping. books siyahısı əvəzinə yalnız title-lar veririk,
    // beləliklə recursion riski olmur və cavab yüngül qalır.
    private AuthorResponseDto toResponse(Author author) {
        List<String> titles = author.getBooks() == null ? List.of() :
                author.getBooks().stream().map(Book::getTitle).toList();

        return AuthorResponseDto.builder()
                .id(author.getId())
                .fullName(author.getFullName())
                .biography(author.getBiography())
                .bookTitles(titles)
                .build();
    }
}
