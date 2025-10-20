package com.linglevel.api.content.article.dto;

import com.linglevel.api.i18n.LanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "아티클 원본 URL 응답")
public class ArticleOriginResponse {

    @Schema(description = "아티클 ID", example = "60d0fe4f5311236168a109ca")
    private String id;

    @Schema(description = "아티클 제목", example = "Viking King's Bizarre Legacy")
    private String title;

    @Schema(description = "원본 URL", example = "https://example.com/article")
    private String originUrl;

    @Schema(description = "타깃 언어 코드 목록", example = "[\"KO\", \"EN\", \"JA\"]")
    private List<LanguageCode> targetLanguageCode;

    @Schema(description = "태그 목록", example = "[\"technology\", \"history\"]")
    private List<String> tags;
}
