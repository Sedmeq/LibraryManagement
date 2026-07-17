package org.example.librarymanagement.service.impl;


import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.dto.BookResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.Author;
import org.example.librarymanagement.entity.Book;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.AuthorRepository;
import org.example.librarymanagement.repository.BookRepository;
import org.example.librarymanagement.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Override
    public BookResponseDto create(BookRequestDto dto) {
        Author author = authorRepository.findById(dto.getAuthorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Author", dto.getAuthorId()));

        Book book = Book.builder()
                .title(dto.getTitle())
                .isbn(dto.getIsbn())
                .publicationYear(dto.getPublicationYear())
                .author(author)
                .build();

        return toResponse(bookRepository.save(book));
    }

    @Override
    @Transactional(readOnly = true)
    public BookResponseDto getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<BookResponseDto> getAll(Pageable pageable) {
        Page<BookResponseDto> page = bookRepository.findAll(pageable).map(this::toResponse);
        return PageResponseDto.from(page);
    }

    @Override
    public BookResponseDto update(Long id, BookRequestDto dto) {
        Book book = findEntity(id);

        if (!book.getAuthor().getId().equals(dto.getAuthorId())) {
            Author newAuthor = authorRepository.findById(dto.getAuthorId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Author", dto.getAuthorId()));
            book.setAuthor(newAuthor);
        }

        book.setTitle(dto.getTitle());
        book.setIsbn(dto.getIsbn());
        book.setPublicationYear(dto.getPublicationYear());

        return toResponse(bookRepository.save(book));
    }

    @Override
    public void delete(Long id) {
        Book book = findEntity(id);
        bookRepository.delete(book);
    }

    private Book findEntity(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", id));
    }

    private BookResponseDto toResponse(Book book) {
        return BookResponseDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .publicationYear(book.getPublicationYear())
                .authorId(book.getAuthor().getId())
                .authorName(book.getAuthor().getFullName())
                .build();
    }
}
