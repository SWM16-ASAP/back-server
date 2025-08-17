package com.linglevel.api.content.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "책 import 요청 DTO")
public class BookImportRequest {
    
    @Schema(description = "S3에 저장된 JSON 파일의 식별자", example = "fdsljfi134")
    private String id;
} 