package org.example.librarymanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loans_member", columnList = "member_id"),
        @Index(name = "idx_loans_book", columnList = "book_id"),
        @Index(name = "idx_loans_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Kitab hələ qaytarılmayıbsa null olur. */
    @Column(name = "return_date")
    private LocalDate returnDate;

    /** DB-də yalnız ACTIVE/RETURNED saxlanılır; OVERDUE runtime-da (dueDate ilə müqayisə) hesablanır. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;
}