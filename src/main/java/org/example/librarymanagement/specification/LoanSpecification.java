package org.example.librarymanagement.specification;

import org.example.librarymanagement.entity.Loan;
import org.example.librarymanagement.entity.LoanStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class LoanSpecification {

    private LoanSpecification() {
    }

    public static Specification<Loan> memberIdEquals(Long memberId) {
        if (memberId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("member").get("id"), memberId);
    }

    public static Specification<Loan> bookIdEquals(Long bookId) {
        if (bookId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("book").get("id"), bookId);
    }

    /** status=OVERDUE üçün xüsusi hal: DB-də saxlanmır, ACTIVE + dueDate < bugün kimi hesablanır. */
    public static Specification<Loan> statusEquals(LoanStatus status) {
        if (status == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        if (status == LoanStatus.OVERDUE) {
            return (root, query, cb) -> cb.and(
                    cb.equal(root.get("status"), LoanStatus.ACTIVE),
                    cb.lessThan(root.get("dueDate"), LocalDate.now()));
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Loan> loanDateFrom(LocalDate from) {
        if (from == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("loanDate"), from);
    }

    public static Specification<Loan> loanDateTo(LocalDate to) {
        if (to == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("loanDate"), to);
    }
}