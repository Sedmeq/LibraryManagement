package org.example.librarymanagement.repository;

import org.example.librarymanagement.entity.Loan;
import org.example.librarymanagement.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    List<Loan> findByMemberIdAndStatus(Long memberId, LoanStatus status);

    long countByMemberIdAndStatus(Long memberId, LoanStatus status);

    boolean existsByBookIdAndMemberIdAndStatus(Long bookId, Long memberId, LoanStatus status);

    @Override
    @EntityGraph(attributePaths = {"book", "book.author", "member"})
    Page<Loan> findAll(Specification<Loan> spec, Pageable pageable);


    @Query("""
            SELECT l FROM Loan l
            JOIN FETCH l.book b
            JOIN FETCH l.member m
            WHERE l.status = org.example.librarymanagement.entity.LoanStatus.ACTIVE
              AND l.dueDate < :today
            ORDER BY l.dueDate ASC
            """)
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);

    @Query("""
            SELECT l FROM Loan l
            JOIN FETCH l.book b
            JOIN FETCH b.author
            WHERE l.member.id = :memberId AND l.status = org.example.librarymanagement.entity.LoanStatus.ACTIVE
            """)
    List<Loan> findActiveLoansByMemberWithBook(@Param("memberId") Long memberId);
}