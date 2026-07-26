package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.LoanRequestDto;
import org.example.librarymanagement.dto.LoanResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface LoanService {

    LoanResponseDto borrowBook(LoanRequestDto dto);

    LoanResponseDto returnBook(Long loanId);

    LoanResponseDto getById(Long id);

    List<LoanResponseDto> getActiveLoansByMember(Long memberId);

    List<LoanResponseDto> getOverdueLoans();

    PageResponseDto<LoanResponseDto> search(Long memberId, Long bookId, String status,
                                            LocalDate loanDateFrom, LocalDate loanDateTo,
                                            Pageable pageable);
}