package org.example.librarymanagement.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanRequestDto {

    @NotNull(message = "memberId boş ola bilməz")
    private Long memberId;

    @NotNull(message = "bookId boş ola bilməz")
    private Long bookId;

    @Min(value = 1, message = "loanPeriodDays ən azı 1 olmalıdır")
    private Integer loanPeriodDays;
}
