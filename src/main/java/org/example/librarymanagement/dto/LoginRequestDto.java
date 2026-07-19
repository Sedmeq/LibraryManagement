package org.example.librarymanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "username boş ola bilməz")
    private String username;

    @NotBlank(message = "password boş ola bilməz")
    private String password;
}
