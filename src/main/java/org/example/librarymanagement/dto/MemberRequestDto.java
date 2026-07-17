package org.example.librarymanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberRequestDto {

    @NotBlank(message = "fullName boş ola bilməz")
    private String fullName;

    @NotBlank(message = "email boş ola bilməz")
    @Email(message = "email formatı yanlışdır")
    private String email;

    @NotNull(message = "membershipDate boş ola bilməz")
    @PastOrPresent(message = "membershipDate gələcək tarix ola bilməz")
    private LocalDate membershipDate;
}
