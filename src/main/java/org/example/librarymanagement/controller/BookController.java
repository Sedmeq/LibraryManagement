package org.example.librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.BookRequestDto;
import org.example.librarymanagement.dto.BookResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.exception.InvalidPaginationException;
import org.example.librarymanagement.service.BookService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Book CRUD əməliyyatları")
public class BookController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "title", "isbn", "publicationYear");

    private final BookService bookService;

    @PostMapping
    @Operation(summary = "Yeni kitab yarat")
    public ResponseEntity<BookResponseDto> create(@Valid @RequestBody BookRequestDto dto,
                                                  UriComponentsBuilder uriBuilder) {
        BookResponseDto created = bookService.create(dto);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/books/{id}").buildAndExpand(created.getId()).toUri())
                .body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Id ilə kitab al")
    public ResponseEntity<BookResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getById(id));
    }

    @GetMapping
    @Operation(
            summary = "Kitab siyahısı (pagination + sorting)",
            parameters = {
                    @Parameter(name = "page", in = ParameterIn.QUERY,
                            description = "Səhifə nömrəsi (0-dan başlayır)",
                            schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
                    @Parameter(name = "size", in = ParameterIn.QUERY,
                            description = "Hər səhifədəki element sayı (max: 100)",
                            schema = @Schema(type = "integer", defaultValue = "10", minimum = "1", maximum = "100")),
                    @Parameter(name = "sort", in = ParameterIn.QUERY,
                            description = "Sıralama: `sahə,asc` və ya `sahə,desc`. İcazə verilənlər: `id`, `title`, `isbn`, `publicationYear`",
                            schema = @Schema(type = "string", defaultValue = "id,asc",
                                    allowableValues = {"id,asc", "id,desc", "title,asc", "title,desc", "isbn,asc", "isbn,desc", "publicationYear,asc", "publicationYear,desc"}))
            }
    )
    public ResponseEntity<PageResponseDto<BookResponseDto>> getAll(
            @Parameter(hidden = true)
            @PageableDefault(size = 10)
            @SortDefault(sort = "id", direction = Sort.Direction.ASC)
            Pageable pageable) {

        validatePageable(pageable, ALLOWED_SORT_FIELDS);
        return ResponseEntity.ok(bookService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kitab məlumatlarını yenilə")
    public ResponseEntity<BookResponseDto> update(@PathVariable Long id,
                                                  @Valid @RequestBody BookRequestDto dto) {
        return ResponseEntity.ok(bookService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kitab sil")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
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
