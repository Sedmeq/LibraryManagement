package org.example.librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.librarymanagement.dto.UserResponseDto;
import org.example.librarymanagement.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * USER və ADMIN rollarının hər ikisinin əlçatan olduğu endpoint nümunəsi.
 * Rol fərqi {@code AdminController} ilə müqayisədə görünür (yalnız ADMIN).
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Cari istifadəçinin öz məlumatları")
public class UserController {

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Cari (login olmuş) istifadəçinin profili — USER və ADMIN üçün əlçatandır")
    public ResponseEntity<UserResponseDto> me(@AuthenticationPrincipal User currentUser) {
        UserResponseDto dto = UserResponseDto.builder()
                .id(currentUser.getId())
                .username(currentUser.getUsername())
                .email(currentUser.getEmail())
                .role(currentUser.getRole().name())
                .build();
        return ResponseEntity.ok(dto);
    }
}
