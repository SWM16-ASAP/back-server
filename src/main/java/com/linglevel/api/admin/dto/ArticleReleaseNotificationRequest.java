package com.linglevel.api.admin.dto;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.i18n.LanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Schema(description = "아티클 출시 알림 전송 요청")
public class ArticleReleaseNotificationRequest {

    @Schema(description = "출시된 아티클 목록", required = true)
    @NotEmpty(message = "Articles are required")
    @Valid
    private List<ArticleInfo> articles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아티클 정보")
    public static class ArticleInfo {

        @Schema(description = "아티클 ID", example = "article-123", required = true)
        @NotBlank(message = "Article ID is required")
        private String articleId;

        @Schema(description = "타겟 언어 코드 목록 (null이면 모든 언어)", example = "[\"KO\", \"EN\"]")
        private List<LanguageCode> targetLanguageCodes;

        @Schema(description = "타겟 카테고리", example = "TECH", required = true)
        @NotNull(message = "Target category is required")
        private ContentCategory targetCategory;
    }
}
