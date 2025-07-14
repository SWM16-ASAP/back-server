package com.linglevel.api.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "책 import 응답 DTO")
public class BookImportResponse {
    
    @Schema(description = "생성된 책의 식별자", example = "60d0fe4f5311236168a109ca")
    private String id;
} 