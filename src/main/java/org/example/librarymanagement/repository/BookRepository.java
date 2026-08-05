package org.example.librarymanagement.repository;

import org.example.librarymanagement.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Page<Book> findByAuthorId(Long authorId, Pageable pageable);

    Optional<Book> findByIsbn(String isbn);

    @Override
    @EntityGraph(attributePaths = {"author", "categories"})
    Page<Book> findAll(Specification<Book> spec, Pageable pageable);

    @Query("""
        SELECT DISTINCT b FROM Book b
        JOIN FETCH b.categories c
        WHERE b.id = :id
        """)
    Optional<Book> findByIdWithCategories(@Param("id") Long id);

    List<Book> findByCategories_NameIgnoreCase(String categoryName);


    @Query(value = """
            SELECT b.* FROM books b
            JOIN loans l ON l.book_id = b.id
            GROUP BY b.id
            ORDER BY COUNT(l.id) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Book> findMostBorrowedBooks(@Param("limit") int limit);

    Page<Book> findByAvailableCopiesGreaterThan(Integer copies, Pageable pageable);



}