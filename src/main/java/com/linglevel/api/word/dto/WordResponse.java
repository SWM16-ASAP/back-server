package com.linglevel.api.word.dto;

import com.linglevel.api.i18n.LanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "단어 응답")
public class WordResponse {
    @Schema(description = "단어 ID", example = "60d0fe4f5311236168a109ca")
    private String id;

    @Schema(description = "원형", example = "see")
    private String originalForm;

    @Schema(description = "변형 형태 타입")
    private VariantType variantType;

    @Schema(description = "원본 언어 코드", example = "en")
    private LanguageCode sourceLanguageCode;

    @Schema(description = "번역 대상 언어 코드", example = "ko")
    private LanguageCode targetLanguageCode;

    @Schema(description = "자주 쓰이는 뜻 3개 요약", example = "[\"보다\", \"알다\", \"이해하다\"]")
    private List<String> summary;

    @Schema(description = "품사별 의미 목록")
    private List<Meaning> meanings;

    @Schema(description = "관련 변형 형태들")
    private RelatedForms relatedForms;

    @Schema(description = "현재 사용자가 해당 단어를 북마크했는지 여부", example = "true")
    private Boolean bookmarked;
}