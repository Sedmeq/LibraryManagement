package org.example.librarymanagement.service;
import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.entity.*;
import org.example.librarymanagement.repository.AuthorRepository;
import org.example.librarymanagement.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@SpringBootTest
class CacheInvalidationTest {

    @Autowired
    private BookService bookService;
    @Autowired
    private CacheManager cacheManager;
    @MockBean
    private BookRepository bookRepository;
    @MockBean
    private AuthorRepository authorRepository;

    @Test
    @DisplayName("Update sonrası cache evict olur, yeni GET DB-yə gedir")
    void update_evictsCache() {
        Book book = Book.builder().id(1L).title("Old Title")
                .author(Author.builder().id(1L).fullName("Author").build())
                .build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookService.getById(1L); // cache-ə düşür
        bookService.getById(1L); // cache-dən gəlir — findById cəmi 1 dəfə çağırılmalıdır

        verify(bookRepository, times(1)).findById(1L);

        BookRequestDto updateDto = new BookRequestDto();
        updateDto.setTitle("New Title");
        updateDto.setIsbn(book.getIsbn());
        updateDto.setAuthorId(1L);
        updateDto.setTotalCopies(1);
        bookService.update(1L, updateDto); // @CacheEvict işə düşməlidir

        bookService.getById(1L); // cache evict olduğu üçün yenidən DB-yə getməlidir
        verify(bookRepository, times(2)).findById(1L);
    }
}