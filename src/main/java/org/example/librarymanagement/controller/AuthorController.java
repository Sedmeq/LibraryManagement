package org.example.librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.AuthorRequestDto;
import org.example.librarymanagement.dto.AuthorResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.exception.InvalidPaginationException;
import org.example.librarymanagement.service.AuthorService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
@Tag(name = "Authors", description = "Author CRUD əməliyyatları")
public class AuthorController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "fullName", "biography");

    private final AuthorService authorService;

    @PostMapping
    @Operation(summary = "Yeni author yarat")
    public ResponseEntity<AuthorResponseDto> create(@Valid @RequestBody AuthorRequestDto dto,
                                                    UriComponentsBuilder uriBuilder) {
        AuthorResponseDto created = authorService.create(dto);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/authors/{id}").buildAndExpand(created.getId()).toUri())
                .body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Id ilə author al")
    public ResponseEntity<AuthorResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(authorService.getById(id));
    }

    @GetMapping
    @Operation(
            summary = "Author siyahısı (pagination + sorting)",
            parameters = {
                    @Parameter(name = "page", in = ParameterIn.QUERY,
                            description = "Səhifə nömrəsi (0-dan başlayır)",
                            schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
                    @Parameter(name = "size", in = ParameterIn.QUERY,
                            description = "Hər səhifədəki element sayı (max: 100)",
                            schema = @Schema(type = "integer", defaultValue = "10", minimum = "1", maximum = "100")),
                    @Parameter(name = "sort", in = ParameterIn.QUERY,
                            description = "Sıralama: `sahə,asc` və ya `sahə,desc`. İcazə verilənlər: `id`, `fullName`, `biography`",
                            schema = @Schema(type = "string", defaultValue = "id,asc",
                                    allowableValues = {"id,asc", "id,desc", "fullName,asc", "fullName,desc", "biography,asc", "biography,desc"}))
            }
    )
    public ResponseEntity<PageResponseDto<AuthorResponseDto>> getAll(
            @Parameter(hidden = true)
            @PageableDefault(size = 10)
            @SortDefault(sort = "id", direction = Sort.Direction.ASC)
            Pageable pageable) {

        validatePageable(pageable, ALLOWED_SORT_FIELDS);
        return ResponseEntity.ok(authorService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Author məlumatlarını yenilə")
    public ResponseEntity<AuthorResponseDto> update(@PathVariable Long id,
                                                    @Valid @RequestBody AuthorRequestDto dto) {
        return ResponseEntity.ok(authorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Author sil")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
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
