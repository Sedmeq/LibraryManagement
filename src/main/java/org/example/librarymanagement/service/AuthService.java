package org.example.librarymanagement.service;

import org.example.librarymanagement.dto.AuthResponseDto;
import org.example.librarymanagement.dto.LoginRequestDto;
import org.example.librarymanagement.dto.RegisterRequestDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto dto);

    AuthResponseDto login(LoginRequestDto dto);
}
