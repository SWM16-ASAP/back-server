package com.linglevel.api.content.news.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "뉴스 임포트 응답")
public class NewsImportResponse {
    
    @Schema(description = "생성된 뉴스 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
}