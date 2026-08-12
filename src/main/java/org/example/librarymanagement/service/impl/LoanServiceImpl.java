package org.example.librarymanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.LoanRequestDto;
import org.example.librarymanagement.dto.LoanResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.Book;
import org.example.librarymanagement.entity.Loan;
import org.example.librarymanagement.entity.LoanStatus;
import org.example.librarymanagement.entity.Member;
import org.example.librarymanagement.exception.BookNotAvailableException;
import org.example.librarymanagement.exception.InvalidLoanOperationException;
import org.example.librarymanagement.exception.MaxActiveLoansExceededException;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.BookRepository;
import org.example.librarymanagement.repository.LoanRepository;
import org.example.librarymanagement.repository.MemberRepository;
import org.example.librarymanagement.service.LoanService;
import org.example.librarymanagement.service.NotificationService;
import org.example.librarymanagement.specification.LoanSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanServiceImpl implements LoanService {

    private static final int MAX_ACTIVE_LOANS_PER_MEMBER = 3;
    private static final int DEFAULT_LOAN_PERIOD_DAYS = 14;

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Override
    public LoanResponseDto borrowBook(LoanRequestDto dto) {
        Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> ResourceNotFoundException.of("Member", dto.getMemberId()));

        Book book = bookRepository.findById(dto.getBookId())
                .orElseThrow(() -> ResourceNotFoundException.of("Book", dto.getBookId()));

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new BookNotAvailableException(
                    "\"" + book.getTitle() + "\" kitabının hazırda mövcud nüsxəsi yoxdur.");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        int periodDays = dto.getLoanPeriodDays() != null ? dto.getLoanPeriodDays() : DEFAULT_LOAN_PERIOD_DAYS;
        LocalDate today = LocalDate.now();

        Loan loan = Loan.builder()
                .book(book)
                .member(member)
                .loanDate(today)
                .dueDate(today.plusDays(periodDays))
                .status(LoanStatus.ACTIVE)
                .build();

        loan = loanRepository.save(loan);

        long activeLoans = loanRepository.countByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE);
        if (activeLoans > MAX_ACTIVE_LOANS_PER_MEMBER) {
            throw new MaxActiveLoansExceededException(
                    "Üzv (id=" + member.getId() + ") maksimum " + MAX_ACTIVE_LOANS_PER_MEMBER +
                            " aktiv icarə limitini aşır. Əməliyyat ləğv edildi.");
        }
        notificationService.sendLoanConfirmationEmail(member.getEmail(), book.getTitle());
        return toResponse(loan);
    }

    @Override
    public LoanResponseDto returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> ResourceNotFoundException.of("Loan", loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new InvalidLoanOperationException("Bu icarə (id=" + loanId + ") artıq qaytarılıb.");
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now());
        loanRepository.save(loan);

        Book book = loan.getBook();
        int newAvailable = Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1);
        book.setAvailableCopies(newAvailable);
        bookRepository.save(book);

        return toResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponseDto getById(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Loan", id));
        return toResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponseDto> getActiveLoansByMember(Long memberId) {
        return loanRepository.findActiveLoansByMemberWithBook(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponseDto> getOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDate.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<LoanResponseDto> search(Long memberId, Long bookId, String status,
                                                   LocalDate loanDateFrom, LocalDate loanDateTo,
                                                   Pageable pageable) {
        LoanStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = LoanStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidLoanOperationException(
                        "Yanlış status dəyəri: '" + status + "'. İcazə verilənlər: ACTIVE, RETURNED, OVERDUE");
            }
        }

        Specification<Loan> spec = Specification
                .where(LoanSpecification.memberIdEquals(memberId))
                .and(LoanSpecification.bookIdEquals(bookId))
                .and(LoanSpecification.statusEquals(statusEnum))
                .and(LoanSpecification.loanDateFrom(loanDateFrom))
                .and(LoanSpecification.loanDateTo(loanDateTo));

        Page<LoanResponseDto> page = loanRepository.findAll(spec, pageable).map(this::toResponse);
        return PageResponseDto.from(page);
    }

    private LoanResponseDto toResponse(Loan loan) {
        boolean overdue = loan.getStatus() == LoanStatus.ACTIVE
                && loan.getDueDate() != null
                && loan.getDueDate().isBefore(LocalDate.now());

        return LoanResponseDto.builder()
                .id(loan.getId())
                .bookId(loan.getBook().getId())
                .bookTitle(loan.getBook().getTitle())
                .memberId(loan.getMember().getId())
                .memberFullName(loan.getMember().getFullName())
                .loanDate(loan.getLoanDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .status(overdue ? LoanStatus.OVERDUE.name() : loan.getStatus().name())
                .build();
    }
}