package org.example.librarymanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRequestDto {

    @NotBlank(message = "title boş ola bilməz")
    private String title;

    @NotBlank(message = "isbn boş ola bilməz")
    @Pattern(regexp = "^[0-9\\-Xx]{10,20}$", message = "isbn formatı yanlışdır")
    private String isbn;

    @NotNull(message = "publicationYear boş ola bilməz")
    @Min(value = 1450, message = "publicationYear 1450-dən böyük olmalıdır")
    private Integer publicationYear;

    @NotNull(message = "authorId boş ola bilməz")
    private Long authorId;
}
