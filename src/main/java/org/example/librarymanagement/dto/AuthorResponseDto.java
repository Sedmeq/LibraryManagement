package org.example.librarymanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


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
