package org.example.librarymanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Entity heç vaxt birbaşa client-ə qaytarılmır - yalnız bu DTO.
// books üçün tam Book entity yox, sadəcə title siyahısı veririk ki,
// Author -> Book -> Author dövrü (infinite recursion) yaranmasın.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorResponseDto {
    private Long id;
    private String fullName;
    private String biography;
    private List<String> bookTitles;
}
