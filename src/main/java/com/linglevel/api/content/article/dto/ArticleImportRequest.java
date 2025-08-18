package com.linglevel.api.content.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "기사 임포트 요청")
public class ArticleImportRequest {
    
    @Schema(description = "S3에 저장된 JSON 파일의 식별자", example = "86781f8a-cb42-4fa1-865e-0e8e20d903d8")
    private String id;
}