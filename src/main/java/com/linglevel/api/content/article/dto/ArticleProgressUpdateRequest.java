package com.linglevel.api.content.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "아티클 읽기 진도 업데이트 요청")
public class ArticleProgressUpdateRequest {
    @Schema(description = "청크 ID", example = "60d0fe4f5311236168c172db")
    private String chunkId;
}