package org.example.librarymanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "username boş ola bilməz")
    @Size(min = 3, max = 50, message = "username 3-50 simvol aralığında olmalıdır")
    private String username;

    @NotBlank(message = "email boş ola bilməz")
    @Email(message = "email formatı yanlışdır")
    private String email;

    @NotBlank(message = "password boş ola bilməz")
    @Size(min = 6, max = 100, message = "password ən azı 6 simvol olmalıdır")
    private String password;
}
