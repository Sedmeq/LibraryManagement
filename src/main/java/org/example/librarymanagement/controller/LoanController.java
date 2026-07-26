package org.example.librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.LoanRequestDto;
import org.example.librarymanagement.dto.LoanResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.exception.InvalidPaginationException;
import org.example.librarymanagement.service.LoanService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Kitab icarə (borrow/return) əməliyyatları")
public class LoanController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "loanDate", "dueDate", "returnDate", "status");

    private final LoanService loanService;

    @PostMapping("/borrow")
    @Operation(summary = "Kitabı icarəyə ver (books.available_copies azalır + loans sətri yaranır, tək transaksiyada)")
    public ResponseEntity<LoanResponseDto> borrow(@Valid @RequestBody LoanRequestDto dto,
                                                  UriComponentsBuilder uriBuilder) {
        LoanResponseDto created = loanService.borrowBook(dto);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/loans/{id}").buildAndExpand(created.getId()).toUri())
                .body(created);
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Kitabı geri qaytar (loan RETURNED olur + books.available_copies artır)")
    public ResponseEntity<LoanResponseDto> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getById(id));
    }

    @GetMapping("/members/{memberId}/active")
    public ResponseEntity<List<LoanResponseDto>> getActiveLoansByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(loanService.getActiveLoansByMember(memberId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<LoanResponseDto>> getOverdueLoans() {
        return ResponseEntity.ok(loanService.getOverdueLoans());
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponseDto<LoanResponseDto>> search(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate loanDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate loanDateTo,
            @Parameter(hidden = true)
            @PageableDefault(size = 10)
            @SortDefault(sort = "id", direction = Sort.Direction.ASC)
            Pageable pageable) {

        validatePageable(pageable, ALLOWED_SORT_FIELDS);
        return ResponseEntity.ok(
                loanService.search(memberId, bookId, status, loanDateFrom, loanDateTo, pageable));
    }

    private void validatePageable(Pageable pageable, Set<String> allowedFields) {
        if (pageable.getPageNumber() < 0) {
            throw new InvalidPaginationException("'page' parametri mənfi ola bilməz.");
        }
        if (pageable.getPageSize() < 1) {
            throw new InvalidPaginationException("'size' parametri ən azı 1 olmalıdır.");
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedFields.contains(order.getProperty())) {
                throw new InvalidPaginationException(
                        String.format("'%s' üzrə sıralama dəstəklənmir. İcazə verilənlər: %s",
                                order.getProperty(), allowedFields));
            }
        }
    }
}