package com.linglevel.api.content.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "기사 임포트 응답")
public class ArticleImportResponse {
    
    @Schema(description = "생성된 기사 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
}