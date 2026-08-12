package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.LoanRequestDto;
import org.example.librarymanagement.dto.LoanResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.*;
import org.example.librarymanagement.exception.BookNotAvailableException;
import org.example.librarymanagement.exception.InvalidLoanOperationException;
import org.example.librarymanagement.exception.MaxActiveLoansExceededException;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.BookRepository;
import org.example.librarymanagement.repository.LoanRepository;
import org.example.librarymanagement.repository.MemberRepository;
import org.example.librarymanagement.service.NotificationService;
import org.example.librarymanagement.service.impl.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanServiceImpl – Unit Testlər")
class LoanServiceImplTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoanServiceImpl loanService;

    private Member member;
    private Book book;
    private Loan activeLoan;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .fullName("Test Üzv")
                .email("test@example.com")
                .membershipDate(LocalDate.of(2023, 1, 1))
                .build();

        book = Book.builder()
                .id(10L)
                .title("Test Kitab")
                .isbn("1234567890")
                .publicationYear(2020)
                .author(Author.builder().id(1L).fullName("Müəllif").build())
                .totalCopies(5)
                .availableCopies(3)
                .build();

        activeLoan = Loan.builder()
                .id(100L)
                .book(book)
                .member(member)
                .loanDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(14))
                .status(LoanStatus.ACTIVE)
                .build();
    }

    // -------------------------------------------------------------------------
    // borrowBook()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("borrowBook: uğurlu icarə - loan yaradılır, availableCopies azalır")
    void borrowBook_success() {
        LoanRequestDto dto = new LoanRequestDto(1L, 10L, 7);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(loanRepository.save(any(Loan.class))).thenReturn(activeLoan);
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(1L);

        LoanResponseDto result = loanService.borrowBook(dto);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getBookId()).isEqualTo(10L);
        assertThat(result.getMemberId()).isEqualTo(1L);
        verify(bookRepository).save(any(Book.class));
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("borrowBook: loanPeriodDays null olduqda default 14 gün istifadə olunur")
    void borrowBook_nullPeriod_usesDefault() {
        LoanRequestDto dto = new LoanRequestDto(1L, 10L, null);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan saved = inv.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(1L);

        LoanResponseDto result = loanService.borrowBook(dto);

        assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(14));
    }

    @Test
    @DisplayName("borrowBook: member tapılmadıqda ResourceNotFoundException atılır")
    void borrowBook_memberNotFound_throws() {
        LoanRequestDto dto = new LoanRequestDto(99L, 10L, 7);
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrowBook(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member");

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("borrowBook: kitab tapılmadıqda ResourceNotFoundException atılır")
    void borrowBook_bookNotFound_throws() {
        LoanRequestDto dto = new LoanRequestDto(1L, 99L, 7);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrowBook(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book");

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("borrowBook: availableCopies 0 olduqda BookNotAvailableException atılır")
    void borrowBook_noAvailableCopies_throws() {
        book.setAvailableCopies(0);
        LoanRequestDto dto = new LoanRequestDto(1L, 10L, 7);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> loanService.borrowBook(dto))
                .isInstanceOf(BookNotAvailableException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("borrowBook: availableCopies null olduqda BookNotAvailableException atılır")
    void borrowBook_nullAvailableCopies_throws() {
        book.setAvailableCopies(null);
        LoanRequestDto dto = new LoanRequestDto(1L, 10L, 7);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> loanService.borrowBook(dto))
                .isInstanceOf(BookNotAvailableException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("borrowBook: max aktiv icarə limiti aşıldıqda MaxActiveLoansExceededException atılır")
    void borrowBook_exceedsMaxActiveLoans_throws() {
        LoanRequestDto dto = new LoanRequestDto(1L, 10L, 7);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(loanRepository.save(any(Loan.class))).thenReturn(activeLoan);
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(4L);

        assertThatThrownBy(() -> loanService.borrowBook(dto))
                .isInstanceOf(MaxActiveLoansExceededException.class);
    }

    // -------------------------------------------------------------------------
    // returnBook()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("returnBook: uğurlu qaytarma - status RETURNED olur, availableCopies artır")
    void returnBook_success() {
        when(loanRepository.findById(100L)).thenReturn(Optional.of(activeLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanResponseDto result = loanService.returnBook(100L);

        assertThat(result.getStatus()).isEqualTo("RETURNED");
        assertThat(result.getReturnDate()).isEqualTo(LocalDate.now());
        verify(loanRepository).save(activeLoan);
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("returnBook: loan tapılmadıqda ResourceNotFoundException atılır")
    void returnBook_notFound_throws() {
        when(loanRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnBook(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan");
    }

    @Test
    @DisplayName("returnBook: artıq qaytarılmış loan üçün InvalidLoanOperationException atılır")
    void returnBook_alreadyReturned_throws() {
        activeLoan.setStatus(LoanStatus.RETURNED);
        activeLoan.setReturnDate(LocalDate.now().minusDays(1));
        when(loanRepository.findById(100L)).thenReturn(Optional.of(activeLoan));

        assertThatThrownBy(() -> loanService.returnBook(100L))
                .isInstanceOf(InvalidLoanOperationException.class);

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("returnBook: availableCopies totalCopies-dən artıq olmur")
    void returnBook_doesNotExceedTotalCopies() {
        book.setTotalCopies(5);
        book.setAvailableCopies(5); // artıq full
        when(loanRepository.findById(100L)).thenReturn(Optional.of(activeLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        loanService.returnBook(100L);

        // Math.min(5, 5+1) = 5 olmalıdır
        assertThat(book.getAvailableCopies()).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // getById()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getById: mövcud loan qaytarılır")
    void getById_found() {
        when(loanRepository.findById(100L)).thenReturn(Optional.of(activeLoan));

        LoanResponseDto result = loanService.getById(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getBookTitle()).isEqualTo("Test Kitab");
        assertThat(result.getMemberFullName()).isEqualTo("Test Üzv");
    }

    @Test
    @DisplayName("getById: mövcud olmayan loan üçün ResourceNotFoundException atılır")
    void getById_notFound_throws() {
        when(loanRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan");
    }

    // -------------------------------------------------------------------------
    // getActiveLoansByMember()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getActiveLoansByMember: aktiv icarələr siyahısı qaytarılır")
    void getActiveLoansByMember_returnsList() {
        when(loanRepository.findActiveLoansByMemberWithBook(1L)).thenReturn(List.of(activeLoan));

        List<LoanResponseDto> result = loanService.getActiveLoansByMember(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("getActiveLoansByMember: aktiv icarə yoxdursa boş siyahı qaytarılır")
    void getActiveLoansByMember_emptyList() {
        when(loanRepository.findActiveLoansByMemberWithBook(1L)).thenReturn(List.of());

        List<LoanResponseDto> result = loanService.getActiveLoansByMember(1L);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getOverdueLoans()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getOverdueLoans: vaxtı keçmiş icarələr qaytarılır")
    void getOverdueLoans_returnsList() {
        Loan overdueLoan = Loan.builder()
                .id(200L)
                .book(book)
                .member(member)
                .loanDate(LocalDate.now().minusDays(30))
                .dueDate(LocalDate.now().minusDays(1))
                .status(LoanStatus.ACTIVE)
                .build();
        when(loanRepository.findOverdueLoans(LocalDate.now())).thenReturn(List.of(overdueLoan));

        List<LoanResponseDto> result = loanService.getOverdueLoans();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("OVERDUE");
    }

    // -------------------------------------------------------------------------
    // search()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("search: düzgün parametrlərlə siyahı qaytarılır")
    void search_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Loan> loanPage = new PageImpl<>(List.of(activeLoan), pageable, 1);
        when(loanRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(loanPage);

        PageResponseDto<LoanResponseDto> result = loanService.search(1L, 10L, "ACTIVE", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("search: yanlış status ilə InvalidLoanOperationException atılır")
    void search_invalidStatus_throws() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> loanService.search(null, null, "INVALID", null, null, pageable))
                .isInstanceOf(InvalidLoanOperationException.class)
                .hasMessageContaining("INVALID");
    }

    @Test
    @DisplayName("search: null status ilə bütün loan-lar qaytarılır")
    void search_nullStatus_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Loan> loanPage = new PageImpl<>(List.of(activeLoan), pageable, 1);
        when(loanRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(loanPage);

        PageResponseDto<LoanResponseDto> result = loanService.search(null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}
