package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.AuthorRequestDto;
import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.dto.BookResponseDto;
import org.example.librarymanagement.dto.LoanRequestDto;
import org.example.librarymanagement.dto.LoanResponseDto;
import org.example.librarymanagement.dto.MemberRequestDto;
import org.example.librarymanagement.entity.LoanStatus;
import org.example.librarymanagement.exception.BookNotAvailableException;
import org.example.librarymanagement.exception.InvalidLoanOperationException;
import org.example.librarymanagement.exception.MaxActiveLoansExceededException;
import org.example.librarymanagement.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LoanService – Integration Testlər (borrow/return + transaction rollback)")
class LoanServiceIntegrationTest {

    @Autowired private LoanService loanService;
    @Autowired private BookService bookService;
    @Autowired private MemberService memberService;
    @Autowired private AuthorService authorService;
    @Autowired private LoanRepository loanRepository;

    private Long authorId;

    @BeforeEach
    void setUp() {
        authorId = authorService.create(
                new AuthorRequestDto("Test Author " + UUID.randomUUID(), "bio")).getId();
    }

    private Long createBook(int totalCopies) {
        String isbn = UUID.randomUUID().toString().replace("-", "").substring(0, 13);
        BookRequestDto dto = new BookRequestDto(
                "Test Book", isbn, 2020, authorId, totalCopies);
        return bookService.create(dto).getId();
    }

    private Long createMember() {
        MemberRequestDto dto = new MemberRequestDto(
                "Test Member", "member-" + UUID.randomUUID() + "@test.local", LocalDate.now());
        return memberService.create(dto).getId();
    }

    @Test
    @DisplayName("borrowBook: uğurlu icarə - availableCopies azalır, loan ACTIVE yaranır")
    void borrowBook_success() {
        Long bookId = createBook(3);
        Long memberId = createMember();

        LoanResponseDto loan = loanService.borrowBook(new LoanRequestDto(memberId, bookId, 7));

        assertThat(loan.getId()).isNotNull();
        assertThat(loan.getStatus()).isEqualTo("ACTIVE");
        assertThat(loan.getDueDate()).isEqualTo(loan.getLoanDate().plusDays(7));
        assertThat(bookService.getById(bookId).getAvailableCopies()).isEqualTo(2);
    }

    @Test
    @DisplayName("borrowBook: mövcud nüsxə qalmadıqda BookNotAvailableException atılır")
    void borrowBook_noAvailableCopies_throws() {
        Long bookId = createBook(1);
        Long memberId = createMember();
        loanService.borrowBook(new LoanRequestDto(memberId, bookId, 7));

        assertThatThrownBy(() -> loanService.borrowBook(new LoanRequestDto(memberId, bookId, 7)))
                .isInstanceOf(BookNotAvailableException.class);
    }

    @Test
    @DisplayName("returnBook: kitab qaytarılır, availableCopies artır, status RETURNED olur")
    void returnBook_success() {
        Long bookId = createBook(2);
        Long memberId = createMember();
        LoanResponseDto loan = loanService.borrowBook(new LoanRequestDto(memberId, bookId, 7));

        LoanResponseDto returned = loanService.returnBook(loan.getId());

        assertThat(returned.getStatus()).isEqualTo("RETURNED");
        assertThat(returned.getReturnDate()).isEqualTo(LocalDate.now());
        assertThat(bookService.getById(bookId).getAvailableCopies()).isEqualTo(2);
    }

    @Test
    @DisplayName("returnBook: artıq qaytarılmış icarə üçün InvalidLoanOperationException atılır")
    void returnBook_alreadyReturned_throws() {
        Long bookId = createBook(2);
        Long memberId = createMember();
        LoanResponseDto loan = loanService.borrowBook(new LoanRequestDto(memberId, bookId, 7));
        loanService.returnBook(loan.getId());

        assertThatThrownBy(() -> loanService.returnBook(loan.getId()))
                .isInstanceOf(InvalidLoanOperationException.class);
    }

    @Test
    @DisplayName("borrowBook: max aktiv icarə limiti aşıldıqda TAM TRANSACTION ROLLBACK olunur")
    void borrowBook_exceedsMaxActiveLoans_rollsBackEverything() {
        Long bookId = createBook(10);
        Long memberId = createMember();

        loanService.borrowBook(new LoanRequestDto(memberId, bookId, 7));
        loanService.borrowBook(new LoanRequestDto(memberId, bookId, 7));
        loanService.borrowBook(new LoanRequestDto(memberId, bookId, 7));

        int availableAfterThree = bookService.getById(bookId).getAvailableCopies();
        long activeLoansAfterThree = loanRepository.countByMemberIdAndStatus(memberId, LoanStatus.ACTIVE);
        assertThat(availableAfterThree).isEqualTo(7);
        assertThat(activeLoansAfterThree).isEqualTo(3);

        assertThatThrownBy(() -> loanService.borrowBook(new LoanRequestDto(memberId, bookId, 7)))
                .isInstanceOf(MaxActiveLoansExceededException.class);

        int availableAfterFailedAttempt = bookService.getById(bookId).getAvailableCopies();
        long activeLoansAfterFailedAttempt = loanRepository.countByMemberIdAndStatus(memberId, LoanStatus.ACTIVE);

        // ROLLBACK SÜBUTU: uğursuz cəhddən sonra vəziyyət 3-cü uğurlu icarə ilə EYNİDİR
        assertThat(availableAfterFailedAttempt).isEqualTo(availableAfterThree);
        assertThat(activeLoansAfterFailedAttempt).isEqualTo(activeLoansAfterThree);
        assertThat(loanRepository.findByMemberIdAndStatus(memberId, LoanStatus.ACTIVE)).hasSize(3);
    }
}