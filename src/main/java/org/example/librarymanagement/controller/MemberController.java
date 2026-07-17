package org.example.librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.MemberRequestDto;
import org.example.librarymanagement.dto.MemberResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.exception.InvalidPaginationException;
import org.example.librarymanagement.service.MemberService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Member CRUD əməliyyatları")
public class MemberController {

    /** Member entity-sinin sıralana bilən sahələri */
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "fullName", "email", "membershipDate");

    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "Yeni üzv yarat")
    public ResponseEntity<MemberResponseDto> create(@Valid @RequestBody MemberRequestDto dto,
                                                    UriComponentsBuilder uriBuilder) {
        MemberResponseDto created = memberService.create(dto);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/members/{id}").buildAndExpand(created.getId()).toUri())
                .body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Id ilə üzv al")
    public ResponseEntity<MemberResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getById(id));
    }

    @GetMapping
    @Operation(
            summary = "Üzv siyahısı (pagination + sorting)",
            parameters = {
                    @Parameter(name = "page", in = ParameterIn.QUERY,
                            description = "Səhifə nömrəsi (0-dan başlayır)",
                            schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
                    @Parameter(name = "size", in = ParameterIn.QUERY,
                            description = "Hər səhifədəki element sayı (max: 100)",
                            schema = @Schema(type = "integer", defaultValue = "10", minimum = "1", maximum = "100")),
                    @Parameter(name = "sort", in = ParameterIn.QUERY,
                            description = "Sıralama: `sahə,asc` və ya `sahə,desc`. İcazə verilənlər: `id`, `fullName`, `email`, `membershipDate`",
                            schema = @Schema(type = "string", defaultValue = "id,asc",
                                    allowableValues = {"id,asc", "id,desc", "fullName,asc", "fullName,desc", "email,asc", "email,desc", "membershipDate,asc", "membershipDate,desc"}))
            }
    )
    public ResponseEntity<PageResponseDto<MemberResponseDto>> getAll(
            @Parameter(hidden = true)
            @PageableDefault(size = 10)
            @SortDefault(sort = "id", direction = Sort.Direction.ASC)
            Pageable pageable) {

        validatePageable(pageable, ALLOWED_SORT_FIELDS);
        return ResponseEntity.ok(memberService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Üzv məlumatlarını yenilə")
    public ResponseEntity<MemberResponseDto> update(@PathVariable Long id,
                                                    @Valid @RequestBody MemberRequestDto dto) {
        return ResponseEntity.ok(memberService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Üzv sil")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------

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
