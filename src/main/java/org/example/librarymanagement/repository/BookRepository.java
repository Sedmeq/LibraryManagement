package org.example.librarymanagement.repository;

import org.example.librarymanagement.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Page<Book> findByAuthorId(Long authorId, Pageable pageable);

    Optional<Book> findByIsbn(String isbn);
}
