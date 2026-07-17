package org.example.librarymanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorRequestDto {

    @NotBlank(message = "fullName boş ola bilməz")
    @Size(min = 2, max = 150, message = "fullName 2-150 simvol arasında olmalıdır")
    private String fullName;

    @Size(max = 2000, message = "biography maksimum 2000 simvol ola bilər")
    private String biography;
}
