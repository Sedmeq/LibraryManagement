package org.example.librarymanagement.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.example.librarymanagement.dto.ErrorResponseDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // Domain exception-ları
    // -------------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicate(DuplicateResourceException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Catches DB-level UNIQUE constraint violations that bypass service-layer checks
     * (e.g. a race condition inserts the same ISBN twice).
     * Returns 409 Conflict instead of leaking a 500 with raw SQL details.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "Unikal dəyər pozulması: eyni dəyərə malik qeyd artıq mövcuddur.", request);
    }

    // -------------------------------------------------------------------------
    // Autentifikasiya (401) / İcazə (403) exception-ları
    // -------------------------------------------------------------------------

    /**
     * 401 UNAUTHORIZED — login zamanı yanlış username/password (BadCredentialsException və s.),
     * və ya controller daxilində atılan digər AuthenticationException-lar.
     * Diqqət: JWT filter səviyyəsindəki 401-lər (token yoxdur/vaxtı bitib) buraya deyil,
     * birbaşa CustomAuthenticationEntryPoint-ə gedir, çünki filter DispatcherServlet-dən əvvəl işləyir.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication(AuthenticationException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "İstifadəçi adı və ya şifrə yanlışdır.", request);
    }

    /**
     * 403 FORBIDDEN — istifadəçi doğrulanıb, amma lazımi rola malik deyil.
     * Adətən CustomAccessDeniedHandler bunu tutur; bu handler yalnız ehtiyat (fallback) rolunu oynayır.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Bu əməliyyat üçün icazəniz yoxdur.", request);
    }

    // -------------------------------------------------------------------------
    // Pagination / Sorting exception-ları
    // -------------------------------------------------------------------------

    /**
     * Xüsusi InvalidPaginationException — controller səviyyəsindən atılır.
     * Yanlış sort sahəsi, mənfi page/size dəyərləri üçün.
     */
    @ExceptionHandler(InvalidPaginationException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidPagination(InvalidPaginationException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Məs: page=abc, size=xyz kimi string göndərildikdə atılır.
     * GET /api/v1/authors?page=abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        String paramName = ex.getName();
        String givenValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "bilinmir";
        String msg = String.format(
                "'%s' parametri üçün '%s' dəyəri yanlışdır. Gözlənilən tip: %s.",
                paramName, givenValue, expectedType);
        return build(HttpStatus.BAD_REQUEST, msg, request);
    }

    /**
     * Məcburi query parametri çatışmadıqda atılır.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParam(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request) {
        String msg = String.format("Məcburi parametr çatışmır: '%s' (%s)", ex.getParameterName(), ex.getParameterType());
        return build(HttpStatus.BAD_REQUEST, msg, request);
    }

    /**
     * Spring Data-nın daxilindən atılan ümumi istifadə xətası.
     * Məs: sort sahəsi mövcud, lakin düzgün navigasiya yolu deyil.
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponseDto> handleDataAccessUsage(InvalidDataAccessApiUsageException ex,
                                                                  HttpServletRequest request) {
        String msg = "Yanlış sorğu parametri. Sort sahəsini yoxlayın.";
        return build(HttpStatus.BAD_REQUEST, msg, request);
    }

    /**
     * Request body-si oxuna bilmədikdə (yanlış JSON formatı, type uyğunsuzluğu).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleNotReadable(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request body oxuna bilmir. JSON formatını yoxlayın.", request);
    }

    // -------------------------------------------------------------------------
    // Validasiya
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        ErrorResponseDto body = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validasiya xətası")
                .path(request.getRequestURI())
                .validationErrors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // -------------------------------------------------------------------------
    // Ümumi fallback
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Gözlənilməz xəta baş verdi. Ətraflı məlumat üçün administratorla əlaqə saxlayın.", request);
    }

    // -------------------------------------------------------------------------
    // Köməkçi metod
    // -------------------------------------------------------------------------

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String message,
                                                   HttpServletRequest request) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}