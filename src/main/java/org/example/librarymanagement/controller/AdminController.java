package org.example.librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.UserResponseDto;
import org.example.librarymanagement.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Yalnız ADMIN roluna açıq endpoint-lər.
 * Rol həm SecurityConfig-də (request matcher), həm də burada method-level
 * {@code @PreAuthorize} ilə iki qat təsdiqlənir.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Yalnız ADMIN roluna açıq əməliyyatlar")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sistemdəki bütün istifadəçilərin siyahısı (yalnız ADMIN)")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userRepository.findAll().stream()
                .map(u -> UserResponseDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .role(u.getRole().name())
                        .build())
                .toList();
        return ResponseEntity.ok(users);
    }
}
